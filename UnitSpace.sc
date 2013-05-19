//This file is part of cbpsc (last revision @ version 0.5).
//
//cbpsc is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
//
//cbpsc is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License along with cbpsc.  If not, see <http://www.gnu.org/licenses/>.
//
// used in cbpsc : created by Thomas Stoll : tms@corpora-sonorus.com : www.corpora-sonorus.com
//
// UnitSpace.sc
// Copyright 2010-11, Thomas Stoll
// based on ParaSpace:
// (c) 2006, Thor Magnusson - www.ixi-software.net
// GNU licence - google it.


UnitSpace {

	var <>unitNodes, <connections, <>chosennode, <>allowConnections, <>highlightedNodes;
	var <>parent, <>bounds, <>xDivisions, <>yDivisions, <>grid;
	var downAction, upAction, trackAction, keyDownAction, rightDownAction, overAction, connAction;
	var background, fillcolor, backgrDrawFunc;
	var nodeCount, <shape;
	var startSelPoint, endSelPoint, refPoint;
	var <selNodes, outlinecolor, selectFillColor, selectStrokeColor, chosencolor, selnodecolor;
	var <mouseTracker, <>conFlag, <>keytracker, <>trackKeys; // experimental
	var nodeSize, swapNode;
	var font, <>fontColor, <>drawStrings;
	var <>zoomedNodes, <>transFactor, <>zoomFactor, zoomDirtyFlag;

	var <>refreshDeferred = false; // if set to true (externally), external caller calls refresh
//	var lazyRefreshFunc;
	var searchFlag = false, <>crossHairs = nil;

	*new { |w, bounds, xdivs, ydivs|
		^super.new.initUnitSpace(w, bounds, xdivs, ydivs);
	}

	initUnitSpace { |argParent, argbounds, xDivs=nil, yDivs=nil|
		var a, b, rect, relX, relY, pen;
		//"SHould be True: ".post; w.isKindOf(ScrollView.implClass).postln;
		parent = argParent;
		(parent.isKindOf(ScrollView.implClass) != true).if { ^nil }; // { parent.acceptsMouseOver = true };
		"initializing UnitSpace".post;

		bounds = argbounds ? Rect(0, 0, 400, 300);
		bounds = Rect(bounds.left + 0.5, bounds.top + 0.5, bounds.width, bounds.height);

		xDivisions = xDivs ? argbounds.width;
		yDivisions = yDivs ? argbounds.height;
		grid = (bounds.width @ bounds.height) / (xDivisions @ yDivisions);
		" with grid size: ".post; grid.postln;

		background = Color.white;
		fillcolor = Color.gray;	//new255(103, 148, 103);
		outlinecolor = Color.red;
		selectFillColor = Color.green(alpha:0.2);
		selectStrokeColor = Color.black;
		chosencolor = Color.green;
		selnodecolor = Color.blue;
		zoomDirtyFlag = true;

		unitNodes = Array[]; // list of UnitNode objects
		connections = Array[]; // list of arrays with connections eg. [2,3]
		// init helper vars
		nodeCount = 0;
		startSelPoint = 0@0;
		endSelPoint = 0@0;
		refPoint = 0@0;
		shape = \rect;
		conFlag = false;
		searchFlag = false;
		nodeSize = 8;
		font = Font("Arial", 9);
		fontColor = Color.black;
		pen	= GUI.pen;
		zoomFactor = 1@1;
		transFactor = 0@0;
		trackKeys = false; // false by default...set to true in subclass to track key strokes

		// tracking the key strokes with an off-screen userView
		keytracker = GUI.userView.new(parent, Rect(-10, -10, 10, 10))
			.canFocus_(trackKeys)
			.keyDownAction_({ |me, key, modifiers, unicode |
				["DOWN", me, key, modifiers, unicode].postln;
				if(unicode == 127, {
					selNodes.do({ |box|
						zoomedNodes.copy.do({ |node, i|
							if(box === node, {this.deleteNode(i)});
						})
					});
				});
				(unicode == 99).if { conFlag = true }; // c is for connecting
				keyDownAction.value(key, modifiers, unicode);
				//this.refresh;
			})
			.keyUpAction_({ |me, key, modifiers, unicode |
				["UP", me, key, modifiers, unicode].postln;
				(unicode == 99).if { conFlag = false }; // c is for connecting
			});

		mouseTracker = GUI.userView.new(parent, Rect(bounds.left, bounds.top, bounds.width, bounds.height))
			.canFocus_(false)
			.mouseDownAction_({|me, x, y, mod|
				(chosennode != nil).if { chosennode.color = fillcolor };
				chosennode = this.findNode(x, y);
				//amongst = nil;
				block { |break|
					selNodes.do({ |sn|
						(sn === chosennode).if // chosen is amongst the sels
						{
							break.value(
								refPoint = x@y; // var used here for reference in trackfunc
								selNodes.do({ |sn|
									sn.refloc = sn.nodeloc.copy;									sn.color = selnodecolor;
								});
								((conFlag == true) && (allowConnections == true)).if
								{ // if selected and "c" then connection is possible
									zoomedNodes.do({ |node, i| (node === chosennode).if { a = i } });
									selNodes.do({ |selnode, j|
										unitNodes.do({ |node, i|
											(node === selnode).if
											{
												b = i;
												this.createConnection(a, b, 1);
											};
										});
									});
								};
							);
						};
					});
					(selNodes != nil).if { selNodes.do({ |sn| sn.color = fillcolor}) };
					selNodes = List[];
				};

				(chosennode !=nil).if
				{ // a node is selected
					chosennode.color = chosencolor;
					downAction.value(chosennode);
					this.refresh;
				} { // no node is selected
					startSelPoint = x@y;
					endSelPoint = x@y;
					this.refresh;
				};
			})
			.mouseMoveAction_({ |me, x, y, mod|
				//selNodes.postln;
				(chosennode != nil).if
				{ // a node is selected
					block {|break|
						selNodes.do({ |sn|
							(sn === chosennode).if   // if the mousedown box is one of selected
							{
								break.value( // then move the whole thing ...
									selNodes.do({ |node| // move selected boxes
										node.setLoc_(node.refloc + (x@y) - refPoint);
									});
								);
							};
						});
						chosennode.setLoc_(Point(x,y)); // called if chosennode not found in selNodes (block never calls break)
					};
					trackAction.value(chosennode);
					this.refresh;
				};
			})
			.mouseUpAction_({ |me, x, y, mod|
				if(chosennode != nil, { // a node is selected
					upAction.value(chosennode, selNodes, me, x, y, mod);
					selNodes.do({ |node| node.refloc = node.nodeloc });
					this.refresh;
				},{ // no node is selected
					// find which nodees are selected
					selNodes = List.new;
					zoomedNodes.do({ |node|
						if(Rect(	startSelPoint.x, // + rect
								startSelPoint.y,									endSelPoint.x - startSelPoint.x,
								endSelPoint.y - startSelPoint.y)
								.containsPoint(node.nodeloc), {
									node.outlinecolor = outlinecolor;
									selNodes.add(node);
						});
						if(Rect(	endSelPoint.x, // - rect
								endSelPoint.y,									startSelPoint.x - endSelPoint.x,
								startSelPoint.y - endSelPoint.y)
								.containsPoint(node.nodeloc), {
									node.outlinecolor = outlinecolor;
									selNodes.add(node);
						});
						if(Rect(	startSelPoint.x, // + X and - Y rect
								endSelPoint.y,									endSelPoint.x - startSelPoint.x,
								startSelPoint.y - endSelPoint.y)
								.containsPoint(node.nodeloc), {
									node.outlinecolor = outlinecolor;
									selNodes.add(node);
						});
						if(Rect(	endSelPoint.x, // - Y and + X rect
								startSelPoint.y,									startSelPoint.x - endSelPoint.x,
								endSelPoint.y - startSelPoint.y)
								.containsPoint(node.nodeloc), {
									node.outlinecolor = outlinecolor;
									selNodes.add(node);
						});
					});
					selNodes.do({ |sn| sn.color = selnodecolor});
					startSelPoint = 0@0;
					endSelPoint = 0@0;
					this.refresh;
				});
			})
			.mouseOverAction_({ |me, x, y|
				overAction.value(x, y);
			})
			.drawFunc_({
				(zoomDirtyFlag == true).if
				{
					//"unitNodes ---> zoomedNodes!".postln;
					zoomedNodes = unitNodes.collect({ |un|
						//[un.nodeloc.x, un.nodeloc.y, this.transFactor.x, this.transFactor.y, un.width, this.zoomFactor.x, un.height, this.zoomFactor.y].postln;
						UnitNode.new(
							(un.nodeloc.x * this.zoomFactor.x) + this.transFactor.x,
							(un.nodeloc.y * this.zoomFactor.y) + this.transFactor.y,
							un.color,
							this,
							un.width * this.zoomFactor.x,
							un.height * this.zoomFactor.y,
							un.state
						);
					});
					zoomDirtyFlag = false;
				};
				pen.width = 1;
				pen.color = background; // background color
				pen.fillRect(bounds); // background fill
				backgrDrawFunc.value; // background draw function
				// grid draw function
				((grid.x > 10) && (grid.y > 10)).if // enforce minimum
				{
					pen.color = Color.gray;
					(1..(xDivisions - 1)).do({ |xd|
						Pen.moveTo( (xd * grid.x) @ bounds.top );
						Pen.lineTo( (xd * grid.x) @ bounds.height );
						pen.stroke;
					});
					(1..(yDivisions - 1)).do({ |yd|
						Pen.moveTo( bounds.left @ (yd * grid.y) );
						Pen.lineTo( bounds.width @ (yd * grid.y) );
						pen.stroke;
					});
				};
				// the lines
				pen.color = Color.black;
				(allowConnections == true).if
				{
					connections.do({ |conn|
						var orig, dest, angle, side;
						(conn[0] != conn[1]).if
						{
							orig = (zoomedNodes[conn[0]].nodeloc + (zoomedNodes[conn[0]].width @ 0) + 0.5);
							dest = zoomedNodes[conn[1]];
							angle = (orig - dest.nodeloc).theta;
							side = ((angle - (pi / 4) % 2pi) * (2 / pi)).floor;
							dest = case
								{ side == 0 } { dest.nodeloc + ((dest.width / 2) @ (dest.height / 2)) }
								{ side == 1 } { dest.nodeloc }
								{ side == 2 } { dest.nodeloc - ((dest.width / 2) @ (dest.height / 2)) }
								{ side == 3 } { dest.nodeloc + (dest.width @ 0) };
							pen.line(orig, dest + 0.5);
							pen.addWedge(dest, 8, angle - (2pi / 16), 2pi / 8)
						};
					});
					pen.stroke;
				};
				// the nodes or circles
				zoomedNodes.do({ |node|
					if(shape == \rect, {
						pen.color = node.color;
						pen.fillRect(node.rect.insetBy(0.5));
						pen.color = Color.black;
						pen.strokeRect(node.rect.insetBy(0.5));
					}, {
						pen.color = node.color;
						pen.strokeOval(node.rect);
					});
					// draw strings, if nec.
					(this.drawStrings != nil).if
					{
						node.string.drawInRect(Rect(node.rect.left+node.size+5,
				    							node.rect.top-3, 80, 16),
											font, fontColor);
					};
				});
				pen.stroke;

				// selection Rect
				pen.width = 1;
				pen.color = selectFillColor;
				// the selection node
				pen.fillRect(Rect(	startSelPoint.x + 0.5,
									startSelPoint.y + 0.5,
									endSelPoint.x - startSelPoint.x,
									endSelPoint.y - startSelPoint.y
									));
				pen.color = selectStrokeColor;
				pen.strokeRect(Rect(	startSelPoint.x + 0.5,
									startSelPoint.y + 0.5,
									endSelPoint.x - startSelPoint.x,
									endSelPoint.y - startSelPoint.y
									));
				(this.crossHairs != nil).if
				{
					this.crossHairs.postln;
					pen.color = Color.green;
					pen.width = 1;
					pen.line((this.crossHairs.x - 12) @ this.crossHairs.y,
						(this.crossHairs.x + 12) @ this.crossHairs.y);
					pen.line(this.crossHairs.x @ (this.crossHairs.y - 12),
						this.crossHairs.x @ (this.crossHairs.y + 12));
					pen.stroke;
				};
				// background frame
				pen.color = Color.black;
				pen.strokeRect(bounds);
			});
	}

	clearSpace {
		unitNodes = Array[];
		connections = Array[];
		nodeCount = 0;
		zoomDirtyFlag = true;
		this.refresh;
	}

	setXDivisions_ { |xd|
		xDivisions = xd;
		grid = (bounds.width @ bounds.height) / (xDivisions @ yDivisions);
		"GRID: ".post; grid.postln;
	}

	setYDivisions_ { |yd|
		yDivisions = yd;
		grid = (bounds.width @ bounds.height) / (xDivisions @ yDivisions);
		"GRID: ".post; grid.postln;
	}

	createConnection { |node1, node2, weight, refresh=true|
		if((nodeCount < node1) || (nodeCount < node2), {
			"Can't connect - there aren't that many nodes".postln;
		}, {
			block {|break|
				connections.do({arg conn;
					if((conn[0..1] == [node1, node2]) || (conn[0..1] == [node2, node1]), {
						break.value;
					});
				});
				// if not broken out of the block, then add the connection
				connections = connections.add([node1, node2, weight]);
				connAction.value(unitNodes[node1], unitNodes[node2], weight);
				(refreshDeferred.not).if {this.refresh};
			}
		});
	}

	deleteConnection { |node1, node2, refresh=true|
		connections.do({ |conn, i|
			((conn[0..1] == [node1, node2]) || (conn[0..1] == [node2, node1])).if
			 { connections = connections.removeAt(i) }
		});
		(refreshDeferred.not).if {this.refresh};
	}

	deleteConnections { // delete all connections
		connections = Array[]; // list of arrays with connections eg. [2,3]
		(refreshDeferred.not).if {this.refresh};
	}

	createNode { |x, y, color, state=nil|
		fillcolor = color ? fillcolor;
		unitNodes = unitNodes.add(UnitNode.new(bounds.left + x, bounds.top + y, fillcolor, this, nil, nil, state));
		//"after createNode call: ".postln; unitNodes.inspect;
		nodeCount = nodeCount + 1;
		zoomDirtyFlag = true;
		(refreshDeferred.not).if { this.refresh };
		^nodeCount
	}

	deleteNode { |nodenr, refresh=true|
		var del = 0;
		connections.copy.do({ |conn, i|
			(conn.includes(nodenr)).if { connections = connections.removeAt((i-del)); del=del+1 };
		});
		connections.do({ |conn,i|
			(conn[0]>nodenr).if {conn[0]=conn[0]-1}; (conn[1]>nodenr).if {conn[1]= conn[1]-1};
		});
		if(unitNodes.size > 0, { unitNodes = unitNodes.removeAt(nodenr) });
		zoomDirtyFlag = true;
		(refreshDeferred.not).if {this.refresh};
	}

	setNodeLoc_ {arg index, argX, argY, refresh=true;
		var x, y;
		x = argX+bounds.left + 0.5;
		y = argY+bounds.top + 0.5;
		unitNodes[index].setLoc_(Point(x, y));
		zoomDirtyFlag = true;
		(refreshDeferred.not).if {this.refresh};
	}

	setNodeLocAction_ {arg index, argX, argY, action, refresh=true;
		var x, y;
		x = argX+bounds.left + 0.5;
		y = argY+bounds.top + 0.5;
		unitNodes[index].setLoc_(Point(x, y));
		switch (action)
			{\down} 	{ downAction.value(unitNodes[index])  }
			{\up} 	{ upAction.value(unitNodes[index])    }
			{\track} 	{ trackAction.value(unitNodes[index]) };
		zoomDirtyFlag = true;
		(refreshDeferred.not).if {this.refresh};
	}

	getNodeLoc { |index|
		var x, y;
		x = unitNodes[index].nodeloc.x - bounds.left;
		y = unitNodes[index].nodeloc.y - bounds.top;
		^(x-0.5 @ y-0.5);
	}

//	getNodeStates {
//		var locs, color, width, height, string;
//		locs = List[]; color = List[]; width = List[]; height = List[]; string = List[];
//		unitNodes.do({ |node|
//			locs.add(node.nodeloc);
//			color.add(node.color);
//			width.add(node.width);
//			height.add(node.height);
//			string.add(node.string);
//		});
//		^[locs, connections, color, width, height, string];
//	}
//
//	setNodeStates_ {arg array; // array with [locs, connections, color, WIDTH, HEIGHT, string]
//		if(array[0].isNil == false, {
//			unitNodes = Array[];
//			array[0].do({arg loc;
//				unitNodes = unitNodes.add(UnitNode.new(loc.x, loc.y, fillcolor, this, nil, nil, nil));
//				nodeCount = nodeCount + 1;
//				})
//		});
//		if(array[1].isNil == false, { connections = array[1];});
//		if(array[2].isNil == false, { unitNodes.do({arg node, i; node.setColor_(array[2][i];)})});
//		((array[3].isNil == false) || (array[4].isNil == false)).if { unitNodes.do({arg node, i; node.setSize_(array[3][i], array[4][i])})};
//		if(array[5].isNil == false, { unitNodes.do({arg node, i; node.string = array[5][i];})});
//		this.refresh;
//	}

	setBackgrColor_ { |color, refresh=true| background = color; (refreshDeferred.not).if {this.refresh} }

	setFillColor_ { |color|
		fillcolor = color;
		unitNodes.do({ |node| node.setColor_(color) });
		zoomDirtyFlag = true;
		(refreshDeferred.not).if { this.refresh };
	}

	setOutlineColor_ { |color| outlinecolor = color; (refreshDeferred.not).if { this.refresh } }

	setSelectFillColor_ { |color, refresh=true| selectFillColor = color; (refreshDeferred.not).if {this.refresh} }

	setSelectStrokeColor_ { |color, refresh=true| selectStrokeColor = color; (refreshDeferred.not).if {this.refresh} }

	setShape_ { |argshape, refresh=true| shape = argshape; (refreshDeferred.not).if {this.refresh} }

	reconstruct { arg aFunc;
		refreshDeferred = true;
		aFunc.value( this );
		this.refresh;
		refreshDeferred = false;
	}

	refresh { { mouseTracker.refresh }.defer }

//	lazyRefresh {
//		if( refreshDeferred.not, {
//			AppClock.sched( 0.02, lazyRefreshFunc );
//			refreshDeferred = true;
//		});
//	}

	setNodeSize_ { |index, width, height|
		unitNodes[index].setSize_(width, height);
		zoomDirtyFlag = true;
		(refreshDeferred.not).if {this.refresh}
	}

	getNodeSize { |index| ^(unitNodes[index].width @ unitNodes[index].height) }

	setNodeColor_ { |index, color, refresh| unitNodes[index].setColor_(color); (refreshDeferred.not).if { this.refresh } }

	getNodeColor { |index| ^unitNodes[index].color }

	setFont_ { |fnt|
		font = fnt;
		zoomDirtyFlag = true;
		(refreshDeferred.not).if { this.refresh }
	 }

	setFontColor_ { |fc|
		fontColor = fc;
		zoomDirtyFlag = true;
		(refreshDeferred.not).if { this.refresh } }

	setNodeState_ { |index, state|
		unitNodes[index].setState_(state);
		zoomDirtyFlag = true;
		(refreshDeferred.not).if { this.refresh }
	}

	setNodeString_ { |index, string|
		unitNodes[index].string = string;
		zoomDirtyFlag = true;
		(refreshDeferred.not).if { this.refresh }
	}

	getNodeString { |index| ^unitNodes[index].string }

	// PASSED FUNCTIONS OF MOUSE OR BACKGROUND
	nodeDownAction_ { |func| downAction = func }

	nodeUpAction_ { |func| upAction = func }

	nodeTrackAction_ { |func| trackAction = func }

	nodeOverAction_ { |func| overAction = func } //; parent.acceptsMouseOver = true!!!!!!!!!!!!

	connectAction_ { |func| connAction = func }

	setMouseOverState_ { |state| parent.acceptsMouseOver = state } // win???

	keyDownAction_ { |func| keyDownAction = func }

	setBackgrDrawFunc_ { |func| backgrDrawFunc = func; this.refresh }

	// local function
	findNode { |x, y|
		var targ = x@y;
		zoomedNodes.do({ |node| (node.rect.containsPoint(targ)).if { ^node } });
		^nil; // if no node found
	}
}

UnitNode {
	var <>fillrect, <state, <>size, <>width, <>height, <>rect, <>nodeloc, <>refloc, <>color, <>outlinecolor;
	var <>spritenum, <>temp;
	var bounds, <>xGrid, <>yGrid;
	var <>string;

	*new { |x, y, color, parent, width, height, state|
		^super.new.initGridNode(x, y, color, parent, width, height, state);
	}

	initGridNode { |argX, argY, argcolor, argparent, argwidth=nil, argheight=nil, argstate=nil |
		bounds = argparent.bounds;
		width = argwidth ? 12;
		height = argheight ? 12;
		size = width @ height;
		xGrid = bounds.width / argparent.xDivisions;
		yGrid = bounds.height / argparent.yDivisions;
		nodeloc = 0@0;
		this.setLoc_(argX@argY);
		state = argstate ? [-1, -1, -1]; // sfid,relid,dur
		string = state[0..1].asString;
		refloc = 0@0;
		color = argcolor;
		outlinecolor = Color.black;
	}

	setLoc_ { |point|
		// keep unitnode inside the bounds
		((point.x) < (bounds.left)).if
			{
				nodeloc.x = (bounds.left / yGrid).floor * yGrid; // we're making the assumption that bnds.top == 0
			} {
				nodeloc.x = (point.x.min(bounds.width - this.width) / yGrid).floor * yGrid;
			};

		((point.y) < (bounds.top)).if
			{
				nodeloc.y = (bounds.top / xGrid).floor * xGrid; // we're making the assumption that bnds.top == 0
			} {
				nodeloc.y = (point.y.min(bounds.height - this.height) / xGrid).floor * xGrid;
			};
		rect = Rect( nodeloc.x, nodeloc.y, width, height);
	}

	setState_ { |argstate| state = argstate; string = state[0..1].asString }

	setSize_ { |wi,ht|
		width = wi; height = ht;
		rect = Rect(nodeloc.x+0.5,  ((nodeloc.y / yGrid).floor * yGrid) + (yGrid / 2) - (height / 2) + 0.5, width, height);
		this.setLoc_(this.nodeloc); // triggers the bounds check!
	}
}
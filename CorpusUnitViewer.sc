CorpusUnitViewer : UnitSpace {
	var <>parent, <>clients;
	var <>corpus, <>cArray, <>xDescr, <>yDescr, <>sDescr;
	var <>groupedTable, >ranges, <>cSearch, <>cTree, <>searchRadius;
	var <>grabbed, <>dragTarget, <>metadataViewer, <>player;
	var <>searchFlag;
	
	*new { |argParent, argBounds, argCorpus, argX, argY, argS|
		^super.new(argParent, argBounds).initCUV(argParent, argBounds, argCorpus, argX, argY, argS);
	}
	
	initCUV { |argParent, argBounds, argCorpus, argX=5, argY=6, argS=7|
		this.parent = argParent;
		this.corpus = argCorpus;
		this.cSearch_(CorpusSearch.new);
		this.descriptors_(argX, argY, argS);
		
		this.setShape_("circ");
		this.drawStrings = nil;
		
		this.nodeDownAction_({ |chosen, selNodes=nil|
			// internal
			this.grabbed = chosen.state;
			"Grabbed: ".post; chosen.state.postln; // change to a drag label
			// external notifications
			(chosen != nil).if { this.clients.do({ |clnt| clnt.value(chosen) }) };
		});

		// redefining to lose the dragging of units!
		this.mouseTracker.mouseMoveAction_({ |me, x, y, mod|
			var newnode = nil;
			// if the search flag is set, searches for and colors found nodes green
			// crossHairs is drawn whenever it is non nil (see the draw func)
			// (x@y).postln;
			(this.searchFlag == true).if
			{
				this.crossHairs = x@y;
//				this.crossHairs.postln;
				(this.chosennode != nil).if { this.chosennode.color = Color.black };
				newnode = this.findNode(x, y);
				(newnode != this.chosennode).if
				{
					this.chosennode = newnode;
					(this.chosennode != nil).if { this.clients.do({ |clnt| clnt.value(chosennode) }) };
				};
				(this.chosennode != nil).if { this.chosennode.color = Color.green };
			} {
				this.crossHairs = nil;
			};
			this.refresh;	// refresh is called every time the mouse moves 1 px...not too efficient
		});
		
		this.nodeUpAction_({ |chosen, selNodes=nil, me, x, y, mod|
			var relMouseUp;
//			"Grabbed: ".post; this.grabbed.post; " ++ ".post; this.dragTarget.post; " ++ ".post; chosen.state.postln;
			((this.grabbed != nil) && (this.dragTarget != nil)).if
			{
				relMouseUp = ((x@y) + this.parent.asView.absoluteBounds.origin);
				(this.dragTarget.mouseTracker.asView.absoluteBounds.contains(relMouseUp)).if
				{
					this.dragTarget.addToScene(this.grabbed.asArray, ((x@y) + this.parent.asView.absoluteBounds.origin - this.dragTarget.mouseTracker.asView.absoluteBounds.origin).asArray );
//					"Adding to scene!".postln;
				};
				this.grabbed = nil;
			};

		});
	}
	
	setSearchFlag_ { |val| this.searchFlag = val; this.refresh }

	ranges {
		this.sync;
		ranges = [this.cArray[this.xDescr].flatten, this.cArray[this.yDescr].flatten, this.cArray[this.sDescr].flatten];
		ranges = [[ranges[0].minItem, ranges[0].maxItem, (ranges[0].maxItem - ranges[0].minItem).abs], [ranges[1].minItem, ranges[1].maxItem, (ranges[1].maxItem - ranges[1].minItem).abs], [ranges[2].minItem, ranges[2].maxItem, (ranges[2].maxItem - ranges[2].minItem).abs]]
		^ranges
	}
	
	descriptors_{ |colX, colY, colS|  	 // updating the 3 viewable descriptors updates the view
		var normedArray;
		this.xDescr = colX;
		this.yDescr = colY;
		this.sDescr = colS;
		
		(this.cArray != nil).if
		{
			this.sync;
//			"building normed array: ".post; this.cArray.postln;
			normedArray = [this.cArray[this.xDescr].flatten.normalize(0.02,0.98), this.cArray[this.yDescr].flatten.normalize(0.02,0.98), this.cArray[this.sDescr].flatten.normalize(0.5,1), this.cArray[2].flatten, this.cArray[3].flatten]; // schema: X,Y,Z, sfid, relid
	
			this.groupedTable = Dictionary[];
			normedArray.flop.do({ |val, ind|	// the grouping
				//val[4].postln;
				//this.groupedTable[val[4]].postln;
				(this.groupedTable[val[3]] == nil).if
				{
					val[3].postln;
					normedArray.flop[ind][0..5].postln;
					val.postln;
					this.groupedTable.add(val[3] -> Dictionary[ind -> normedArray.flop[ind][0..5]]);
				} {
					this.groupedTable[val[3]].add(ind -> normedArray.flop[ind][0..5]);
				};	// completly hashed
			});
			this.initSearchTree;
			this.renderView;
		}
		^this
	}
	
	sync {
		var temp = this.corpus.mapSoundFileUnitsToCorpusUnits, ca;
//		"temp: ".post; temp.postln;
		(temp.size > 0).if {
			ca = Array[];
			temp.keys.asArray.sort.do({ |cid|
				ca = ca.add(temp[cid]);
			});
			//"CA: ".post; ca.postln;
			this.cArray = ca.flop;
		};
//		"SYNC: ".post; this.cArray.postln;
	}

	initSearchTree {
		var arrayedTable = Array[];
		// groupedTable has to have been formed at this point
		(groupedTable != nil).if
		{
			groupedTable.keysValuesDo({ |key, val|
				val.do({ |cell, index|
					arrayedTable = arrayedTable.add([cell[0], cell[1], [key.asInteger, index.asInteger] ]);
				});
			});
			//arrayedTable.postcs;
			this.cTree = this.cSearch.buildTree(arrayedTable, true);
			//this.cTree.dumpTree;
			this.searchRadius_(0.08);
		};
	}

	renderView {
		var nc;
//		"Rendering View; CHECK SLOT 3 and 4!".postln;
		this.clearSpace;
		this.groupedTable.keysValuesDo({ |sfid, dict|
			dict.keysValuesDo({ |relid, unit|
				//unit.postln;
				//unit[3..4].asString.postln;
				nc = this.createNode(unit[0] * this.bounds.width, unit[1] * this.bounds.height);
				this.setNodeSize_((nc - 1), unit[2] * 8, unit[2] * 8);
				this.setNodeState_((nc - 1), unit[3..4]);
			});
		});
		//this.refresh;
	}
	
	highlight { |pair|
		this.unitNodes.do({ |node|
			node.outlinecolor = Color.black;
			((node.state[0] == pair[0]) && (node.state[1] == pair[1])).if
			{
				node.outlinecolor = outlinecolor;
			};
		});
	//this.refresh
	}
}
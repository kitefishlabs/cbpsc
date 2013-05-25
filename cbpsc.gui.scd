//This file is part of cbpsc (last revision @ version 1.0).


//
//cbpsc is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
//
//cbpsc is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License along with cbpsc.  If not, see <http://www.gnu.org/licenses/>.
//
// cbpsc : created by Thomas Stoll : tms@corpora-sonorus.com : www.corpora-sonorus.com


//
// Copyright 2010-13, Thomas Stoll

// 1. evaluate the parenthesized functions

(
~newSoundFileUnitRow = { |currfile, pid, parent, top, bot|
	var currSFNode = ~crps.sfTree.nodes[ ~crps.sfMap[currfile] ], foo, sfu;
	[currfile, pid, parent, top, bot].postln;
	[ Button.new(parent, Rect(0, (pid * 25), 22, 22))
			.states_([[pid.asString, Color.blue, Color.white], [pid.asString, Color.white, Color.blue]])
			.value_(0)
			.action_({ |bttn|
				~updateSFViews.value(currSFNode.sfID, top, bot, pid);
				~updateMFCCSpectrum.value(currSFNode.sfID, pid, 1);
			}),
		Button.new(parent, Rect(25, (pid * 25), 22, 22))
			.states_([["X", Color.white, Color.red]])
			.value_(0)
			.mouseUpAction_({ |num| AppClock.sched(0, {|now| ~deleteSoundFileUnit.value(pid, top, bot); nil}) }),
		Button.new(parent, Rect(50, (pid * 25), 22, 22))
			.states_([["P", Color.black, Color.green]])
			.value_(0)
			.mouseUpAction_({ ~playSegment.value([currSFNode.sfID, pid]) }),

		sfu = currSFNode.unitSegments[pid];
		NumberBox.new(parent, Rect(75, (pid * 25), 60, 20))
			.value_(currSFNode.unitSegments[pid].onset)
		    .clipLo_(0.0)
    		.clipHi_(currSFNode.duration)
		    .decimals_(3)
			.action_({|val|
			    "ONSET VAL: ".post; val.value.postln;
				~crps.updateSoundFileUnit(currSFNode.sfID, pid, onset:val.value);
				~crps.segmentUnits(currSFNode.sfID);
    			~updateSoundFileUnitRowViews.value(currSFNode.sfID, bot);
				~updateSFViews.value(currSFNode.sfID, top, bot, pid);
//				~updateCurrentSoundFileUnit.value(pid,1);
    			~gui[\soundFileUnitRowViews][pid][5].value_(([sfu.onset, sfu.duration] ++ currSFNode.unitMFCCs[pid][0..]).flatten.collect({|x|x.asStringPrec(3)}));
    			~updateMFCCSpectrum.value(currSFNode.sfID, pid, 1);
			}),
		NumberBox.new(parent, Rect(140, (pid * 25), 60, 20))
			.value_(currSFNode.unitSegments[pid].duration)
		    .clipLo_(0.0)
    		.clipHi_(currSFNode.duration)
		    .decimals_(3)
			.action_({|val|
			    "DUR VAL: ".post; val.value.postln;
				~crps.updateSoundFileUnit(currSFNode.sfID, pid, duration:val.value);
				~crps.segmentUnits(currSFNode.sfID);
    			~updateSoundFileUnitRowViews.value(currSFNode.sfID, bot);
				~updateSFViews.value(currSFNode.sfID, top, bot, pid);
//				~updateCurrentSoundFileUnit.value(pid,1);
				~gui[\soundFileUnitRowViews][pid][5].value_(([sfu.onset, sfu.duration] ++ currSFNode.unitMFCCs[pid][0..]).flatten.collect({|x|x.asStringPrec(3)}));
    			~updateMFCCSpectrum.value(currSFNode.sfID, pid, 1);
			}),
		TextField.new(parent, Rect(205, (pid * 25), 700, 20))
			.value_(
			([sfu.onset, sfu.duration] ++ currSFNode.unitMFCCs[pid][0..]).flatten.collect({|x|x.asStringPrec(3)}).postln;
			([sfu.onset, sfu.duration] ++ currSFNode.unitMFCCs[pid][0..]).flatten.collect({|x|x.asStringPrec(3)})),
		pid.asInteger ];
};

~updateSoundFileUnitRowViews = { |sfid, bot|
	var usegs = ~crps.sfTree.nodes[sfid].unitSegments;
	var waveformArea = ~gui[\tabs][1][\areas][\waveform];

	usegs.do({|sfu, rid|

		~deleteSoundFileUnit.value(rid, waveformArea[\fullView], waveformArea[\closeView]); // |did, top, bot|
	});
	usegs.do({|sfu, rid|
		~newSoundFileUnitRow.value(sfid, rid, ~gui[\tabs][1][\areas][\segments][\view], waveformArea[\fullView], waveformArea[\closeView]); // |currfile, pid, parent, top, bot|
	});
};

~updateSoundFileUnitRow = { |rid, bot|
	var theunit = ~crps.sfTree.nodes[ ~crps.sfMap[ ~gui[\cf] ] ].unitSegments[rid];
	Post << "<updateSoundFileUnitRow> theunit: " << theunit << "  rid: " << rid << "\n";
	~gui[\soundFileUnitRowViews][rid][3].value_(theunit.onset); ////!!!!!!!!!
	~gui[\soundFileUnitRowViews][rid][4].value_(theunit.duration);
	bot.setSelection(rid, [0, 0]);
	bot.setSelection(rid, [theunit.onset, theunit.duration] * 44100);
	//"Row ".post; rid.post; " updated.".postln;
};

~updateSFViews = { |cfpath, top, bot, currid|
	var theunits = ~crps.sfTree.nodes[ ~crps.sfMap[ ~gui[\cf] ] ].unitSegments;
	[cfpath, top, bot, currid].postln;
	Post << "[updateSFViews] theunits: " << "\n";
	theunits.do({ |u| [u.onset, u.duration].postln; });
	// clear Views
	(0..63).do({ |i| top.selectNone(i) });
	bot.selectNone(0);
	// read out units into the two views if they exist
	(theunits.size > 0).if
	{
		theunits.do({ |sfu, index| top.setSelection(index, [sfu.onset, sfu.duration] * 44100) });
		bot.setSelection(0, ([theunits[currid].onset, theunits[currid].duration] * 44100));
		//unselect
		Post << "CURR: " << ~gui[\cp] << " ... " << "NEXT: " << currid << "\n";
		(~gui[\soundFileUnitRowViews][~gui[\cp]].isNil.not).if { ~gui[\soundFileUnitRowViews][~gui[\cp]][0].value_(0)};
		//update
		~gui[\cp] = currid;
		~gui[\soundFileUnitRowViews][~gui[\cp]][0].value_(1);
		//~updateCurrentSoundFileUnit.value(currid,1);
	};
};

~updateMFCCSpectrum = { |currfileid, pid, tab| // better: call with curr id int!!!
	var currSFNode = ~crps.sfTree.nodes[ currfileid ], foo;
	// Post << "curr. file: " << currfileid << "\n";
	// currSFNode.unitMFCCs[pid].ampdb.postln;

	(~gui[\tabs][tab][\areas][\highlighter][\view].children != nil).if { ~gui[\tabs][tab][\areas][\highlighter][\view].children.do({ |child| child.remove; child.free }) };
	~gui[\tabs][tab][\areas][\highlighter].add(\ms -> MultiSliderView.new(~gui[\tabs][tab][\areas][\highlighter][\view], Rect(0,0,125,225)));

	// currSFNode.unitMFCCs[pid].postln; "::::".postln;
	// (currSFNode.unitMFCCs[pid].ampdb / 32).class.postln;
	foo = (1 + (currSFNode.unitMFCCs[pid].ampdb / 32)).min(1.0).max(0.0);

	~gui[\tabs][tab][\areas][\highlighter][\ms].size = 225 / 25 ;
	~gui[\tabs][tab][\areas][\highlighter][\ms].acceptsMouse_(False);
	// ~gui[\tabs][tab][\areas][\highlighter][\ms].size.postln;
	~gui[\tabs][tab][\areas][\highlighter][\ms].value = foo[0];
	// "1".postln;
	~gui[\tabs][tab][\areas][\highlighter][\ms].isFilled_(true); // width in pixels of each stick

	// "2".postln;
	~gui[\tabs][tab][\areas][\highlighter][\ms].indexThumbSize_(225 / 25 * 0.2); // spacing on the value axis
	// "3".postln;
	~gui[\tabs][tab][\areas][\highlighter][\ms].gap_((225 / 25 * 0.8));
	// "4".postln;
	~gui[\tabs][tab][\areas][\highlighter][\ms].indexIsHorizontal_(false);
	// "5".postln;
	~gui[\tabs][tab][\areas][\highlighter][\view].refresh;
	// "6".postln;

};

~updateCurrentSoundFileUnit = { |cpuid|
	//unselect
	~gui[\soundFileUnitRowViews][~gui[\cp]][0].value_(0);
	//update
	~gui[\cp] = cpuid;
	~gui[\soundFileUnitRowViews][~gui[\cp]][0].value_(0);
};

~deleteSoundFileUnit = { |did, top, bot|
	var range, num = ~gui[\soundFileUnitRowViews][did].pop;
	~gui[\soundFileUnitRowViews][did].do({|item| item.remove; item.free });
	top.setSelection(~gui[\cp], [0, 0]);
	top.setSelection((~gui[\soundFileUnitRowViews].size - 1), [0, 0]);
	bot.setSelection(0, [0, 0]);
	~gui[\soundFileUnitRowViews].add(did->nil);
	~graphNeedsUpdate = true;

	range = ~crps.removeSoundFileUnit(~crps.sfMap[ ~gui[\cf] ], did);
//	"Popping this provisional unit: ".post; num.postln;
//	"Range: ".post;	range.postln;
	(range != nil).if
	{
		range.do({ |rv|
			var waveformArea = ~gui[\tabs][1][\areas][\waveform];
			var tmp = ~newSoundFileUnitRow.value(~gui[\cf], rv, ~gui[\tabs][1][\areas][\segments][\view], waveformArea[\fullView], waveformArea[\closeView]); // the data model has already been updated!!!!
			~gui[\soundFileUnitRowViews].add(rv -> tmp);
			~gui[\soundFileUnitRowViews][rv + 1].pop;
			~gui[\soundFileUnitRowViews][rv + 1].do({|item| item.remove; item.free });
			~gui[\soundFileUnitRowViews].add((rv + 1) -> nil);
		});
	};

	(~gui[\soundFileUnitRowViews].size > 0).if
	{
		~updateCurrentSoundFileUnit.value(~gui[\cp] % ~gui[\soundFileUnitRowViews].size);
		(~gui[\cp]..(~gui[\soundFileUnitRowViews].size - 1)).do({ |prow|
			top.setSelection(prow, [~crps.sfTree.nodes[ ~crps.sfMap[ ~gui[\cf] ] ].unitSegments[prow].onset, ~crps.sfTree.nodes[ ~crps.sfMap[ ~gui[\cf] ] ].unitSegments[prow].duration] * 44100);


		});
		bot.setSelection(0, [~crps.sfTree.nodes[ ~crps.sfMap[ ~gui[\cf] ] ].unitSegments[~gui[\cp]].onset, ~crps.sfTree.nodes[ ~crps.sfMap[ ~gui[\cf] ] ].unitSegments[~gui[\cp]].duration] * 44100);
	};
	~crps.segmentUnits(~crps.sfMap[ ~gui[\cf] ]);
	~updateSoundFileUnitRowViews.value(~crps.sfMap[ ~gui[\cf] ], bot);
};

~buildGraphParams = { |tabnum, xx, yy, ss, dragtarget = nil, fontcolor, initSelection|



	var descrids = Array[], selected;
	var graph = ~gui[\tabs][tabnum][\areas][\graph], graphparams = ~gui[\tabs][tabnum][\areas][\graphParams];
	var gpheight = graphparams[\view].bounds.height;

	~crps.dTable.keys.asArray.sort.do({ |descr| descrids = descrids.add(~crps.dTable[descr].asString) });
	~gui[\tabs][tabnum][\xx] = ~gui[\tabs][tabnum][\xx] ? xx;
	~gui[\tabs][tabnum][\yy] = ~gui[\tabs][tabnum][\yy] ? yy;
	~gui[\tabs][tabnum][\ss] = ~gui[\tabs][tabnum][\ss] ? ss;

	~gui[\tabs][tabnum][\areas][\graphParams][\view].children.do({|child| child.remove; child.free });
	"BUILDING GRAPH PARAMS!!!".postln;

	graph.sync;
	graph.descriptors_(xx, yy, ss).fontColor_(fontcolor).dragTarget_(dragtarget).zoomFactor_(1@1).transFactor_(0@0);
	graph.clients_([	// unitData Viewer, player, MFCC plotter
		{ |chosen1| "::: ".post; [chosen1.state[0], chosen1.state[1]].postln; graphparams[\unitData].items_(~crps.getSoundFileUnitMetadata(chosen1.state[0], chosen1.state[1])[0..20].collect({|x| x.asStringPrec(3)})) },	// link scene to metadata view
		{ |chosen2| ~playSegment.value(chosen2.state) },
		{ |chosen3|
			[chosen3.state[0], chosen3.state[1]].postln;
			~updateMFCCSpectrum.value(~crps.sfMap[ chosen3.state[0] ], chosen3.state[1], 2);
//			Plotter(parent: ~gui[\tabs][tabnum][\areas][\highlighter])
//				.value_(~crps.sfTree.nodes[ chosen3.state[0] ].unitMFCCs[chosen3.state[1]].ampdb)
//				.plotMode_(\plines)


//				.findSpecs_(true);


		}

	]);
	graph.highlight([0,0]);
	// the 4 columns: names, mins, currently selected unit, maxes
	graphparams[\minList].remove;
	graphparams[\unitData].remove;
	graphparams[\maxList].remove;

//	~gui[\tabs][2][\areas][\graph].cArray.postln;

	graphparams.add(\dList -> ListView(graphparams[\view], Rect(0, 25, 80, gpheight - 25))
			.stringColor_(Color.white).selectedStringColor_(Color.green)


			.items_(~crps.dTable.keys.asArray.sort.collect({ |descr| ~crps.dTable[descr].asString })))
		.add(\xLabel -> StaticText(graphparams[\view], Rect(85, 5, 80, 20))
			.string_("X").stringColor_(Color.green))



		.add(\minList -> ListView(graphparams[\view], Rect(85, 25, 80, gpheight - 25))
			.stringColor_(Color.white).selectedStringColor_(Color.green)
			.items_(~gui[\tabs][tabnum][\areas][\graph].cArray[0..20].collect({|col| col.minItem.asStringPrec(3) }))
			.value_(~gui[\tabs][tabnum][\xx])
			.action_({ |selection|
				graph.descriptors_(selection.value.max(0).min(descrids.size),
					~gui[\tabs][tabnum][\areas][\graphParams][\unitData].value,
					~gui[\tabs][tabnum][\areas][\graphParams][\maxList].value
				)
			}))


		.add(\yLabel -> StaticText(graphparams[\view], Rect(170, 5, 80, 20))
			.string_("Y").stringColor_(Color.green))



		.add(\unitData -> ListView(graphparams[\view], Rect(170, 25, 80, gpheight - 25))
			.stringColor_(Color.white).selectedStringColor_(Color.green)
			.items_(~gui[\tabs][2][\areas][\graph].cArray.flop[0][0..20].collect({|item| item.asStringPrec(3) }))
			.value_(~gui[\tabs][tabnum][\yy])
			.action_({ |selection|
				graph.descriptors_(~gui[\tabs][tabnum][\areas][\graphParams][\minList].value,
					selection.value.max(0).min(descrids.size),
					~gui[\tabs][tabnum][\areas][\graphParams][\maxList].value
				)
			}))


		.add(\sLabel -> StaticText(graphparams[\view], Rect(255, 5, 80, 20))
			.string_("S").stringColor_(Color.green))



		.add(\maxList -> ListView(graphparams[\view], Rect(255, 25, 80, gpheight - 25))
			.stringColor_(Color.white).selectedStringColor_(Color.green)
			.items_(~gui[\tabs][tabnum][\areas][\graph].cArray[0..20].collect({|col| col.maxItem.asStringPrec(3) }))
			.value_(~gui[\tabs][tabnum][\ss])
			.action_({ |selection|
				graph.descriptors_(~gui[\tabs][tabnum][\areas][\graphParams][\minList].value,
					~gui[\tabs][tabnum][\areas][\graphParams][\unitData].value,
					selection.value.max(0).min(descrids.size)
				)
			}));
};



~scheduleUpdates = { |num|
	(num == 1).if { [2].do({ |n| ~gui[\tabs][n][\needsUpdate] = true }) } { ~gui[\tabs][num][\needsUpdate] = true };
};



// pair = [sfid, relid]
~playSegment = { |pair|
	var cf, relid = pair[1], node, sfunit, offset, dur;
	(pair[0].isNumber).if { cf = ~crps.sfMap[ pair[0] ] } { cf = pair[0] };
	"Playing unit number: ".post; relid.post; ", from ".post; cf.postln;

	node = ~crps.sfTree.nodes[ ~crps.sfMap[~cf] ];
	sfunit = node.unitSegments[relid];

	Post << "offset: " << sfunit.onset << " dur: " << sfunit.duration << "\n";

	(node.buffer.isNil.not).if
	{
		"PLAYING MONO".postln;
		~audio[\monoPlayer].set(
			\bufNum, ~crps.sfTree.nodes[ ~crps.sfMap[~cf] ].buffer.bufnum,
			\offset, sfunit.onset,
			\dur, sfunit.duration);	// msecs -> secs
		~audio[\mBus].set(1);


	} {};/*{



		"PLAYING STEREO".postln;
		~audio[\stereoPlayer].set(\bufNumL, ~crps[\sftrees][ cf ].tree[\bfrL].bufnum, \bufNumR, ~crps[\sftrees][ cf ].tree[\bfrR].bufnum, \offset, (~crps[\sfutable][cf][\keys][relid][4] * 0.001), \dur, (~crps[\sfutable][cf][\keys][relid][5] * 0.001));	// msecs -> secs
		~audio[\sBus].set(1);
	}*/
};
)

////***********************************************************************************************


////		Load Audio Server + Send SynthDefs




// 2., 3., 4. evaluate the following 3 lines
Server.default = Server.internal;
~audio = Dictionary[];
~audio.add(\server -> Server.default.boot);



// 5. evaluate the following to send 2 synthdefs to the server
(
SynthDef.new(\unitSamplerMono, { |out=0, busTrig=1, bufNum, transp=1, offset=0,attack=0.01,dur=1,release=0.5, pan=0, da=0|
	var env, chunk, inTrig;
	inTrig = InTrig.kr(busTrig);
	env = EnvGen.kr(Env.linen(attack, (dur - attack - release), release, 1), gate: inTrig, doneAction: da);
	chunk = PlayBuf.ar(1, bufNum, transp, inTrig, (offset.max(0).min(BufDur.kr(bufNum) - dur) * BufSampleRate.kr(bufNum)));
	Out.ar( out, Pan2.ar(chunk * env, pan) );
}).send(~audio[\server]);

SynthDef.new(\unitSamplerStereo, { |out=0, busTrig=1, bufNumL, bufNumR, transp=1, offset=0,attack=0.01,dur=1,release=0.5, pan=0, da=0|
	var env, chunkL, chunkR, inTrig;
	inTrig = InTrig.kr(busTrig);
	env = EnvGen.kr(Env.linen(attack, (dur - attack - release), release, 1), gate: inTrig, doneAction: da);
	chunkL = PlayBuf.ar(1, bufNumL, transp, inTrig, (offset.max(0).min(BufDur.kr(bufNumL) - dur) * BufSampleRate.kr(bufNumL)) );
	chunkR = PlayBuf.ar(1, bufNumR, transp, inTrig, (offset.max(0).min(BufDur.kr(bufNumR) - dur) * BufSampleRate.kr(bufNumR)));
	Out.ar( out, [chunkL, chunkR] * env );
}).send(~audio[\server]);


)



////***********************************************************************************************


////		Init Database + Build GUI...



// 6. evaluate the following to launch the GUI

// 7. CHANGE THE FIRST ARGUMENT TO ANY STRING YOU LIKE

(
~crps = CorpusDB.new("/Users/kfl/mycorpus/", s);

~audio.add(\mBus -> Bus.control(s, 1));
~audio.add(\monoPlayer -> Synth.new(\unitSamplerMono, [\busTrig, ~audio[\mBus]]));
~audio.add(\sBus -> Bus.control(s, 1));
~audio.add(\stereoPlayer -> Synth.new(\unitSamplerStereo, [\busTrig, ~audio[\sBus]]));



////***********************************************************************************************
////		Begin GUI code
////
//// window + top level container (tabbedView) + top level container views and storage

~gui = Dictionary[];
~gui.add(\topWindow -> Window((~crps.anchor.asString ++ " db"), Rect(305, 75, 1120, 840)).acceptsMouseOver_(true));
~gui.add(\tabs -> Dictionary[]);
~gui.add(\cf -> 0)		// 'helper' vars w/ more-or-less global scope
	.add(\cp -> 0)
	.add(\fileListDict -> Dictionary[])
	.add(\soundFileUnitRowViews -> Dictionary[])
	.add(\latestSFID -> -1);

~gui[\tabs].add(\view -> TabbedView( ~gui[\topWindow], nil, ["import","analyze","graph/db"], [Color.gray(0.25, 0.5)])



	.tabPosition_(\top).tabWidth_(70).tabHeight_(13).tabCurve_(3)
	.backgrounds_([Color.gray(0.2)]).stringColor_(Color.white).stringFocusedColor_(Color.green));

~gui[\tabs][\view].views.do({ |vw, ix|
	~gui[\tabs].add(ix -> Dictionary[\nameTag -> [\import, \analyze, \graph][ix], \view -> QScrollView(~gui[\tabs][\view].views[ix], ~gui[\tabs][\view].views[ix].asView.bounds), \areas -> Dictionary[], \needsUpdate -> true])
});

//// some additional behaviors for the top level views


~gui[\topWindow].onClose_( {|ind| [~audio[\mBus], ~audio[\sBus], ~audio[\monoPlayer], ~audio[\stereoPlayer], ~crps].do(_.free) } );

//// containers/areas



~gui[\tabs][0][\areas].add(\filesList -> Dictionary[\view -> CompositeView(~gui[\tabs][0][\view], Rect(0, 0, 415, 440)).background_(Color.black)])
	.add(\addedFilesList -> Dictionary[\view -> CompositeView(~gui[\tabs][0][\view], Rect(500, 0, 415, 440)).background_(Color.black)]);

~gui[\tabs][1][\areas].add(\buttons -> Dictionary[\view -> CompositeView(~gui[\tabs][1][\view], Rect(10, 0, 1100, 20)).background_(Color.black)])
	.add(\waveform -> Dictionary[\view -> CompositeView(~gui[\tabs][1][\view], Rect(10, 20, 1100, 160)).background_(Color.black)])
	.add(\analysis -> CompositeView(~gui[\tabs][1][\view], Rect(10, 185, 1100, 410)).background_(Color.black))
	.add(\segBtns -> Dictionary[\view -> CompositeView(~gui[\tabs][1][\view], Rect(0, 600, 25, 215)).background_(Color.black)])
	.add(\segments -> Dictionary[\view -> ScrollView(~gui[\tabs][1][\view], Rect(30, 600, 945, 215)).background_(Color.black)])
	.add(\highlighter -> Dictionary[\view -> CompositeView(~gui[\tabs][1][\view], Rect(980, 600, 130, 225)).background_(Color.yellow)]);

~gui[\tabs][2][\areas].add(\graph -> CorpusUnitViewer(~gui[\tabs][2][\view], Rect(0,0, 640, 640), ~crps, 5, 6, 7))
	.add(\graphParams -> Dictionary[\view -> CompositeView(~gui[\tabs][2][\view], Rect(650, 5, 360, 400)).background_(Color.black)])
	.add(\unitPlayer -> Dictionary[\view -> CompositeView(~gui[\tabs][2][\view], Rect(650, 650, 360, 160)).background_(Color.black)])
	.add(\highlighter -> Dictionary[\view -> CompositeView(~gui[\tabs][2][\view], Rect(650, 405, 125, 225))]);

//// waveform area
~gui[\tabs][1][\areas][\waveform].add(\fullView -> SoundFileView(~gui[\tabs][1][\areas][\waveform][\view], Rect(0, 0, 1080, 60))
	.acceptsMouse_(False)
	.gridColor_(Color.gray)
	.action_()
	)
	.add(\zoomBar -> RangeSlider(~gui[\tabs][1][\areas][\waveform][\view], Rect(0, 60, 1080, 20))
	.lo_(0.0).hi_(1.0)
	.mouseUpAction_({ |slider|
		var zaRangeLo, zaRangeHi, zoomArray, temp;
		//"slider: ".post; slider.lo.post; slider.hi.postln;
		var cview = ~gui[\tabs][1][\areas][\waveform][\closeView];
		cview.zoomToFrac(1);
		cview.scrollToStart;
		cview.zoom(slider.hi - slider.lo);
		cview.scroll(slider.lo / (slider.hi - slider.lo));
		zoomArray = ~crps.mfccs[ ~crps.sfMap[ ~gui[\cf] ] ].flop;
		zaRangeLo = (slider.lo * zoomArray[0].size).floor.asInteger;
		zaRangeHi = (slider.hi * zoomArray[0].size).ceil.asInteger;
		temp = zoomArray.collect({ |row| row[zaRangeLo..zaRangeHi] });

		~gui[\tabs][1][\areas][\analysis].children.do(_.remove);
		Plotter(parent: ~gui[\tabs][1][\areas][\analysis])
		.value_(temp)
		.plotMode_(\linear)
		.superpose_(false)
		.findSpecs_(true)

	});
)
	.add(\closeView -> SoundFileView(~gui[\tabs][1][\areas][\waveform][\view], Rect(0, 80, 1080, 80))

		.gridColor_(Color.gray)
		.mouseUpAction_({ |vw,x,y|
			// update the data model
	        [~crps.sfMap[ ~gui[\cf] ], ~gui[\cp],
        		(~gui[\tabs][1][\areas][\waveform][\closeView].selectionStart(0) / 44100.0).max(0),
       			(~gui[\tabs][1][\areas][\waveform][\closeView].selectionSize(0) / 44100.0)].postln;
	        ~crps.updateSoundFileUnit(~crps.sfMap[ ~gui[\cf] ], ~gui[\cp],
			  	(~gui[\tabs][1][\areas][\waveform][\closeView].selectionStart(0) / 44100.0).max(0),
    			(~gui[\tabs][1][\areas][\waveform][\closeView].selectionSize(0) / 44100.0));

			// update number boxes + fullView
			~updateSoundFileUnitRow.value(~gui[\cp], ~gui[\tabs][1][\areas][\waveform][\fullView]);

    		~crps.segmentUnits( ~crps.sfMap[ ~gui[\cf] ] );
    		~updateSoundFileUnitRowViews.value(~crps.sfMap[ ~gui[\cf] ], ~gui[\tabs][1][\areas][\waveform][\closeView]);

			~gui[\soundFileUnitRowViews].keys.do({ |pid|
			~gui[\soundFileUnitRowViews][pid][5].value_(([~crps.sfTree.nodes[ ~crps.sfMap[ ~gui[\cf] ] ].unitSegments[pid].onset, ~crps.sfTree.nodes[ ~crps.sfMap[ ~gui[\cf] ] ].unitSegments[pid].duration] ++ ~crps.sfTree.nodes[ ~crps.sfMap[ ~gui[\cf] ] ].unitMFCCs[pid][0..]).flatten.collect({|x|x.asStringPrec(3)})); // $$$$
			});
		~updateMFCCSpectrum.value(~crps.sfMap[ ~gui[\cf] ], ~gui[\cp], 1);


			~graphNeedsUpdate = true;
		})
	);




//// filesList area
~gui[\tabs][0][\areas][\filesList].add(\addFileButton -> Button(~gui[\tabs][0][\areas][\filesList][\view], Rect(5, 5, 45, 20))


		.states_([["+file", Color.black, Color.white]])
		.value_(0)
		.action_({ |bttn|
			Dialog.getPaths({ |paths|
				paths.do({ |pth|
					var sf, thepath;
					var fileListView = ~gui[\tabs][0][\areas][\filesList][\fileListView];
					sf = SoundFile.openRead(pth.asString);
					//"SF: ".post; sf.post; " with ".post; sf.numChannels.post; " channels.".postln;
					(sf != nil).if
					{
						thepath = PathName.new(sf.path.asString);
					    thepath.fileName.postln;
						~gui[\latestSFID] = ~crps.addSoundFile(thepath.fileName, importFlag:true).sfID;
						fileListView.items.postln;
					    fileListView.items_((fileListView.items ++ [thepath.fileName]).asSet.asArray.sort);
    					fileListView.items.postln;
    					~gui[\topWindow].refresh;
						~gui[\fileListDict].add(thepath.fileName -> thepath.fullPath);
						sf.close;
					};
				});
			},{	"cancelled".postln }, false);
		}))
	.add(\addFolderButton -> Button(~gui[\tabs][0][\areas][\filesList][\view], Rect(55, 5, 45, 20))
		.states_([["+dir", Color.black, Color.white]])
		.value_(0)
		.action_({ |bttn|
			Dialog.getPaths({ |paths|
				(paths.size != 0).if
				{
					paths.do({ |pth|
						var counter, fileListView, sf, thepath, thefiles, theentries, candidateFile, mdFlag;
						fileListView = ~gui[\tabs][0][\areas][\filesList][\fileListView];
						sf = SoundFile.openRead(pth.asString);
						(sf != nil).if
						{
							thepath = PathName.new(sf.path.asString);
							thefiles = PathName.new(thepath.pathOnly.asString).files;
							theentries = PathName.new(thepath.pathOnly.asString).files;
							counter = thefiles.size;
							AppClock.sched(0.001,
							{
								(counter > 0).if
								{
									candidateFile = thefiles[counter - 1].fileName;
									mdFlag = ".md.".matchRegexp(candidateFile.asString);
									Post << "FILE: " << candidateFile << "... MDFLAG: " << mdFlag << "\n";
									(mdFlag == false).if
									{
										~gui[\latestSFID] = ~crps.addSoundFile(candidateFile, importFlag:true).sfID;
										fileListView.items_((fileListView.items ++ [theentries[counter - 1].fileName]).asSet.asArray.sort);
										~gui[\fileListDict].add(theentries[counter - 1].fileName -> theentries[counter - 1].fullPath);
									};
									counter = counter - 1;
									0.001
								} { sf.close; nil };
							});
						};
					});
				};
			},{
				"cancelled".postln;
			}, false);
		}))
	.add(\removeFileButton -> Button(~gui[\tabs][0][\areas][\filesList][\view], Rect(105, 5, 45, 20))
		.states_([["-file", Color.red, Color.white]])
		.value_(0)
		.action_({ |btn|
			var findex = ~gui[\tabs][0][\areas][\filesList][\fileListView].value;
			var fname = ~gui[\tabs][0][\areas][\filesList][\fileListView].items[ findex ];
			var tmp = ~gui[\tabs][0][\areas][\filesList][\fileListView].items;
			tmp.removeAt(findex);
			~gui[\tabs][0][\areas][\filesList][\fileListView].items_(tmp);
			~crps.removeSoundFile(~gui[\fileListDict][fname]);
			~gui[\fileListDict].add(fname -> nil);
			~gui[\tabs][0][\areas][\filesList][\fileListView].valueAction_(0);
		}))
	.add(\importButton -> Button(~gui[\tabs][0][\areas][\filesList][\view], Rect(155, 5, 45, 20))
		.states_([["import", Color.black, Color.white]])
		.value_(0)
		.action_({ |btn|
			Dialog.getPaths({ |paths|
				paths.do({ |pth|
					var thepath = PathName.new(pth.asString).fullPath.asString, tmp = Array[];
					var fileListView = ~gui[\tabs][0][\areas][\filesList][\fileListView];
					~crps.importCorpusFromXML(~audio[\server], thepath);
					~crps.sfTree.nodes.keys.do({|filename|
						tmp = tmp ++ [PathName.new(filename).fileName];
						~gui[\fileListDict].add(PathName.new(filename).fileName -> PathName.new(filename).fullPath);
					});

					fileListView.items_((fileListView.items ++ tmp).asSet.asArray.sort);

					~graphNeedsUpdate = true; // ???


				});


			},{	"cancelled".postln }, false);
		}))
	.add(\exportButton -> Button(~gui[\tabs][0][\areas][\filesList][\view], Rect(205, 5, 45, 20))
		.states_([["export", Color.black, Color.white]])
		.value_(0)
		.action_({ |btn|
			Dialog.savePanel({ |path|
				~gui[\tabs][0][\areas][\filesList][\dropUnanalyzedButton].action.value;
				~crps.exportCorpusToXML(~audio[\server], path);
			},{
				"cancelled".postln;
			});
		}))
	.add(\dropUnanalyzedButton -> Button(~gui[\tabs][0][\areas][\filesList][\view], Rect(255, 5, 120, 20))
		.states_([["-- drop unanalyzed", Color.red, Color.white]])
		.value_(0)
		.action_({ |bttn|
			var tmp = Array[], fileListArea = ~gui[\tabs][0][\areas][\filesList][\fileListView];
			fileListArea.items.do({ |file|
				(~crps[\sftrees][~gui[\fileListDict][file.asString]].tree[\abfr] != nil).if
				{
					tmp = tmp.add(file.asString);
				} {
					"Dropping: ".post; file.asString.postln;
					~crps.removeSoundFile(~gui[\fileListDict][file.asString]);
				};
			});
			fileListArea.items_(tmp.asSet.asArray.sort);
			fileListArea.enabled = true;
		}))
// left hand side file list
	.add(\fileListView -> ListView(~gui[\tabs][0][\areas][\filesList][\view], Rect(5, 30, 400, 400))
    	.background_(Color.black)
	    .stringColor_(Color.white)
		.selectedStringColor_(Color.green)
		.items_([])
		.action_({ |q|
			var cf, path, file, ext, mdpath, sf, entry;
			// declare abbreviations for areas of the gui to be accessed:
			var waveformArea = ~gui[\tabs][1][\areas][\waveform], analysisArea = ~gui[\tabs][1][\areas][\analysis], fileListArea = ~gui[\tabs][0][\areas][\filesList], addedFileListArea = ~gui[\tabs][0][\areas][\addedFilesList];
			// empty ~soundFileUnits out

			~gui[\soundFileUnitRowViews].do({ |line| line.pop; line.do({ |elem| elem.remove; elem.free }) });
			~gui[\soundFileUnitRowViews] = Dictionary[];
			// get the menu selection and set current file gui-level var

			entry = fileListArea[\fileListView].items.removeAt(q.value);
		    Post << "entry: " << entry << "\n";
			cf = ~gui[\fileListDict][ entry ];

			fileListArea[\fileListView].items_(fileListArea[\fileListView].items);
			fileListArea[\fileListView].refresh;

			//fileListView.items_((fileListView.items ++ [thepath.fileName]).asSet.asArray.sort);

			addedFileListArea[\addedFileListView].items_((addedFileListArea[\addedFileListView].items ++ [entry]).asSet.asArray.sort);
			~gui[\tabs][1][\areas][\buttons][\corpusFiles].items_(~gui[\tabs][1][\areas][\buttons][\corpusFiles].items ++ [entry]);


			~gui[\cf] = cf;
			// if non-nil, read out cached units into sfunitrows $$$$
		    "units... ".postln;
			(~crps.sfTree.nodes[ ~crps.sfMap[ ~gui[\cf] ] ].unitSegments.size > 0).if
			{
				" from cache...".postln;
				~crps.sfTree.nodes[ ~crps.sfMap[cf] ].do({ |pu, ix|
					~gui[\soundFileUnitRowViews] = ~gui[\soundFileUnitRowViews].add(ix ->
						~newSoundFileUnitRow.value(
						    ~crps.sfMap[ ~gui[cf]],
							ix,
							~gui[\tabs][1][\areas][\segments][\view],
							waveformArea[\fullView],
							waveformArea[\closeView]
						);
					)
				});
			} {
				"...units not cached... ".postln;
			};

			// read sound file and stream or post analysis data
			sf = SoundFile.new;
			(sf.openRead(cf) == true).if
			{
				fileListArea[\fileListView].enabled = false;
				waveformArea[\fullView].soundfile = sf;
					waveformArea[\fullView].read(0, sf.numFrames);
				waveformArea[\closeView].soundfile = sf;
				waveformArea[\closeView].read(0, sf.numFrames);
				"window should refresh now a!".postln;
				~gui[\topWindow].refresh;

			    "$$$".postln;
    			cf.postln;
    			~cf = cf;
			    ~crps.mfccs.postln;
                ~crps.sfTree.nodes[ ~crps.sfMap[cf] ].buffer.postln;

      			(~crps.mfccs[ ~crps.sfMap[cf] ].isNil).if
				{	// stream it
					{~crps.analyzeSoundFile(sfID:~crps.sfMap[cf], verbose:true)}.fork(SystemClock);
					// schedule the updates
					{
						while({
						    (~crps.mfccs[ ~crps.sfMap[cf] ].isNil);
						},{
							~crps.sfTree.nodes[ ~crps.sfMap[cf] ].buffer.postln;
							0.1.wait;
						});
						0.01.wait;
						analysisArea.children.do(_.remove);

						Plotter(parent: analysisArea)
							.value_(~crps.mfccs[ ~crps.sfMap[cf] ].flop) //(~modArray)  //[0]
							.plotMode_(\plines)


							.superpose_(false)
							.findSpecs_(true)


							.editFunc_({|pl, pli, idx, val|


								pli.post; "...".post; idx.post; "....".post; val.postln;
								~crps.mfccs[ ~crps.sfMap[cf] ][pli][idx] = val;
							});


						fileListArea[\fileListView].enabled = true;
					}.fork(AppClock);

				} { // post it if cached
					analysisArea.children.do(_.remove);
					Plotter(parent: analysisArea)
						.value_(~crps.mfccs[ ~crps.sfMap[cf] ].flop)  //[0]
						.plotMode_(\plines)
						.superpose_(false)
						.findSpecs_(true)
						.editFunc_({|pl, pli, idx, val|
							pli.post; "...".post; idx.post; "....".post; val.postln;
							~crps.mfccs[ ~crps.sfMap[cf] ][pli][idx] = val;
						});
					AppClock.sched(0, { |tm| fileListArea[\fileListView].enabled = true });
				};
			};
			// update the sfviews (close and full) no matter what
			~updateSFViews.value(~gui[\cf], waveformArea[\fullView], waveformArea[\closeView], 0);
		}));


// ListView for added files
~gui[\tabs][0][\areas][\addedFilesList].add(\addedFileListView -> ListView(~gui[\tabs][0][\areas][\addedFilesList][\view], Rect(5, 30, 400, 400))
	.stringColor_(Color.white)
	.items_([]));


//// segments area
// + button
~gui[\tabs][1][\areas][\segBtns].add(\plusButton -> Button(~gui[\tabs][1][\areas][\segBtns][\view], Rect(2, 5, 20, 20))

	.states_([["+",Color.black,Color.white]])
	.value_(0)
	.mouseUpAction_({ |bttn|
		var np, tmp, cf = ~gui[\cf];
		var waveformArea = ~gui[\tabs][1][\areas][\waveform];
		Post << "**CF: " << cf << "\n";
		(~crps.sfTree.nodes[ ~crps.sfMap[cf] ].unitSegments.size == 0).if
		{
			np = 0;
			~gui[\latestSFID] = ~crps.addSoundFileUnit(~crps.sfMap[cf], 0, 0.5);
		} { // else
			tmp = ~crps.sfTree.nodes[ ~crps.sfMap[cf] ].unitSegments.collect({ |sfu| [sfu.onset, sfu.duration] }).sort({|a,b| a[0] > b[0]});
			np = ~crps.sfTree.nodes[ ~crps.sfMap[cf] ].unitSegments.size;
			~crps.addSoundFileUnit(~crps.sfMap[cf], tmp[0][0] + tmp[0][1], 0.5);
		};

		~crps.segmentUnits(~crps.sfMap[cf]);
		tmp = ~newSoundFileUnitRow.value(cf, np, ~gui[\tabs][1][\areas][\segments][\view], waveformArea[\fullView], waveformArea[\closeView]);
		~gui[\soundFileUnitRowViews] = ~gui[\soundFileUnitRowViews].add(np -> tmp);
		waveformArea[\fullView].setSelection(np, ~crps.sfTree.nodes[ ~crps.sfMap[cf] ].unitSegments.collect({ |sfu| [sfu.onset, sfu.duration] }).sort({|a,b| a[0] > b[0]}) * 44100);
		~scheduleUpdates.value(1);

		(~crps.sfTree.nodes[ ~crps.sfMap[cf] ].unitSegments.size == 1).if { ~gui[\soundFileUnitRowViews][0][0].value_(0); ~gui[\soundFileUnitRowViews][0][0].valueAction_(1) };

	});
);
// -> button
~gui[\tabs][1][\areas][\segBtns].add(\fillButton -> Button(~gui[\tabs][1][\areas][\segBtns][\view], Rect(2, 30, 20, 20))
	.states_([["->",Color.black,Color.white]])
	.value_(0)
	.mouseUpAction_({ |bttn|
		var last = ~gui[\soundFileUnitRowViews].size - 1;
		var sfile = ~gui[\cf];
		var sfid = ~crps.sfMap[ ~gui[\cf] ];
		var waveformArea = ~gui[\tabs][1][\areas][\waveform];
		// update the last one, filling to duration of sfile
		~crps.updateSoundFileUnit(~crps.sfMap[ ~gui[\cf] ], last,
			duration: (~crps.sfTree.nodes[ ~crps.sfMap[ ~gui[\cf] ] ].buffer.duration - (~crps.getUnitSegment(sfid, last).onset + ~crps.getUnitSegment(sfid, last).duration)));//### replacement for getSFUM
		(last > 0).if
		{
			((last - 1)..0).do({ |pid|
				~crps.updateSoundFileUnit(~crps.sfMap[ ~gui[\cf] ], pid,
					duration: (~crps.getUnitSegment(sfid, (pid + 1)).onset - ~crps.getUnitSegment(sfid, pid).onset));
			});
		};
		~gui[\soundFileUnitRowViews].do({ |line| line.pop; line.do({ |elem| elem.remove; elem.free }) });
		~gui[\soundFileUnitRowViews] = Dictionary[];

		(~crps[\sfutable][~gui[\cf]][\keys] != nil).if
		{
			~crps[\sfutable][~gui[\cf]][\keys].do({ |pu, ix|
				~gui[\soundFileUnitRowViews] = ~gui[\soundFileUnitRowViews].add(ix ->
					~newSoundFileUnitRow.value(
						~gui[\cf],
						ix,
						~gui[\tabs][1][\areas][\segments][\view],
						waveformArea[\fullView],
						waveformArea[\closeView]
					);
				)
			});
		};
		~updateSFViews.value(~gui[\cf], waveformArea[\fullView], waveformArea[\closeView], 0);
	});
);


// corpus files dropdown
~gui[\tabs][1][\areas][\buttons].add(\corpusFiles -> PopUpMenu.new(~gui[\tabs][1][\areas][\buttons][\view], Rect(100, 0, 100, 20))
	.items_([])
	.stringColor_(Color.white)
	.action_({ |menu|
//		~gui[\fileListDict][ menu.item ].postln;

		var cf, path, file, ext, mdpath, sf, entry;

			// declare abbreviations for areas of the gui to be accessed:
			var waveformArea = ~gui[\tabs][1][\areas][\waveform], analysisArea = ~gui[\tabs][1][\areas][\analysis], fileListArea = ~gui[\tabs][0][\areas][\filesList], addedFileListArea = ~gui[\tabs][0][\areas][\addedFilesList];
			// empty ~soundFileUnits out

			~gui[\soundFileUnitRowViews].do({ |line| line.pop; line.do({ |elem| elem.remove; elem.free }) });
			~gui[\soundFileUnitRowViews] = Dictionary[];
			// get the menu selection and set current file gui-level var

//			entry = fileListArea[\fileListView].items[q.value];

			cf = ~gui[\fileListDict][ menu.item ];

//			fileListArea[\fileListView].items_(fileListArea[\fileListView].items);
//			fileListArea[\fileListView].refresh;

			//fileListView.items_((fileListView.items ++ [thepath.fileName]).asSet.asArray.sort);

//			addedFileListArea[\addedFileListView].items_((addedFileListArea[\addedFileListView].items ++ [entry]).asSet.asArray.sort);
//			~gui[\tabs][1][\areas][\buttons][\corpusFiles].items_(~gui[\tabs][1][\areas][\buttons][\corpusFiles].items ++ [entry]);

			~gui[\cf] = cf;
			// if non-nil, read out cached units into sfunitrows
			"units... ".postln;
			(~crps.sfTree.nodes[cf] != nil).if
			{
				" from cache...".postln;
				~crps.sfTree.nodes[cf].unitSegments.do({ |pu, ix|
					~gui[\soundFileUnitRowViews] = ~gui[\soundFileUnitRowViews].add(ix ->
						~newSoundFileUnitRow.value(
							~gui[\cf],
							ix,
							~gui[\tabs][1][\areas][\segments][\view],
							waveformArea[\fullView],
							waveformArea[\closeView]
						);
					)
				});
			} {
				"...units not cached... ".postln;
			};

			// read sound file and stream or post analysis data
			sf = SoundFile.new;
			(sf.openRead(cf) == true).if
			{
				fileListArea[\fileListView].enabled = false;
				waveformArea[\fullView].soundfile = sf;
					waveformArea[\fullView].read(0, sf.numFrames);
				waveformArea[\closeView].soundfile = sf;
				waveformArea[\closeView].read(0, sf.numFrames);
				"window should refresh now b!".postln;
				~gui[\topWindow].refresh;

                "$$$".postln;
				(~crps.mfccs[ ~crps.sfMap[cf] ] == nil).if
				{	// stream it
					{~crps.analyzeSoundFile(cf, sfid:~gui[\latestSFID], verbose:true)}.fork(SystemClock);
					// schedule the updates
					{
						while({
							(~crps.sfTree.nodes[ ~crps.sfMap[cf] ].buffer == nil);
						},{
							~crps.sfTree.nodes[ ~crps.sfMap[cf] ].buffer.postln;
							0.1.wait;
						});
						0.01.wait;
						analysisArea.children.do(_.remove);

//						~origArray = ~crps.mfccs[ ~crps.sfMap[currfile] ];
//						~modArray = [];
//						~descriptors = [0,1,2,7].sort;
//						~origArray.flop.do({ |col,index| (~descriptors.asSet.includes(index)).if { ~modArray = ~modArray ++ col.bubble } });
//						//~modArray.postcs;

						Plotter(parent: analysisArea)
							.value_(~crps.mfccs[ ~crps.sfMap[cf] ].flop) //(~modArray)  //[0]
							.plotMode_(\plines)


							.superpose_(false)
							.findSpecs_(true)


							.editFunc_({|pl, pli, idx, val|


								pli.post; "...".post; idx.post; "....".post; val.postln;
								~crps.mfccs[ ~crps.sfMap[cf] ][pli][idx] = val;
							});


						fileListArea[\fileListView].enabled = true;
					}.fork(AppClock);

				} { // post it if cached
					"RAW DESCRIPTORS ARE NOT NIL!".postln;
					analysisArea.children.do(_.remove);
					Plotter(parent: analysisArea)
						.value_(~crps.mfccs[ ~crps.sfMap[cf] ].flop)  //[0]
						.plotMode_(\plines)


						.superpose_(false)


						.findSpecs_(true)


						.editFunc_({|pl, pli, idx, val|
							pli.post; "...".post; idx.post; "....".post; val.postln;
							~crps.mfccs[ ~crps.sfMap[cf] ][pli][idx] = val;


						});
					AppClock.sched(0, { |tm| fileListArea[\fileListView].enabled = true });
				};
			};
			// update the sfviews (close and full) no matter what
			~updateSFViews.value(~gui[\cf], waveformArea[\fullView], waveformArea[\closeView], 0);

	}));


////====================================================================================
////
////		Graph View (tabs.views[2])

~gui[\tabs][\view].focusActions[2] = {
	"focussing!".postln;
	(~gui[\tabs][2][\needsUpdate] == true).if
	{
		"init the graph".postln;
		~buildGraphParams.value(2, 6, 7, 8, nil, Color.blue);
		~gui[\tabs][2][\needsUpdate] = false;
	}
};

~gui[\tabs][2][\areas][\unitPlayer].add(\linkButton -> Button.new( ~gui[\tabs][2][\areas][\unitPlayer][\view], Rect(5, 5, 60, 20))
	.states_([["touch", Color.black, Color.white], ["search", Color.black, Color.green]])
	.value_(0)
	.mouseUpAction_({ |btn|
		btn.value.postln;
		(btn.value == 1).if
		{
			~gui[\tabs][2][\areas][\graph].setSearchFlag_(false);
		} {
			~gui[\tabs][2][\areas][\graph].setSearchFlag_(true);
		};
	});
);



// specify plot layout
GUI.skin.plot.gridLinePattern = FloatArray[2, 1];
GUI.skin.plot.fontColor = Color.white;
GUI.skin.plot.gridColorX = Color.gray(0.75, 0.25);
GUI.skin.plot.gridColorY = Color.gray(0.75, 0.25);
GUI.skin.plot.background = Color.black;
GUI.skin.plot.plotColor = Color.blue;
GUI.skin.plot.labelX = nil;
GUI.skin.plot.labelY = nil;




//// display the window + run "app"


~gui[\topWindow].front;



)
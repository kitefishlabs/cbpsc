

//This file is part of cbpsc (last revision @ version 0.4).


//
//cbpsc is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
//
//cbpsc is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License along with cbpsc.  If not, see <http://www.gnu.org/licenses/>.
//
// cbpsc : created by Thomas Stoll : tms@corpora-sonorus.com : www.corpora-sonorus.com


//
// Copyright 2010-11, Thomas Stoll

// 1. evaluate the parenthasized functions
(
~newSoundFileUnitRow = { |currfile, pid, parent, top, bot|
	var currSFU = ~crps[\sfutable][currfile], foo;
	[ Button.new(parent, Rect(0, (pid * 25), 22, 22))
			.states_([[pid.asString, Color.blue, Color.white], [pid.asString, Color.white, Color.blue]])
			.value_(0)
			.action_({ |bttn|
				~updateSFViews.value(currfile, top, bot, pid);
				~updateMFCCSpectrum.value(currfile, pid, 1);
			}),
		Button.new(parent, Rect(25, (pid * 25), 22, 22))
			.states_([["X", Color.white, Color.red]])
			.value_(0)
			.mouseUpAction_({ |num| AppClock.sched(0, {|now| ~deleteSoundFileUnit.value(pid, top, bot); nil}) }),
		Button.new(parent, Rect(50, (pid * 25), 22, 22))
			.states_([["P", Color.black, Color.green]])
			.value_(0)
			.mouseUpAction_({ ~playSegment.value([currfile, pid]) }),


		NumberBox.new(parent, Rect(75, (pid * 25), 60, 20))


			.value_(currSFU[\keys][pid][4])
			.action_({|val| // "ONSET VAL: ".post; val.value.postln;
				~crps.updateSoundFileUnit(currfile, pid, nil, val.value.floor);
				~crps.segmentUnits(currfile);
				~updateSFViews.value(currfile, top, bot, pid);
//				~updateCurrentSoundFileUnit.value(pid,1);
				~gui[\soundFileUnitRowViews][pid][5].value_((currSFU[\keys][pid][0..]++currSFU[\mfccs][pid][0..]).asString);
			}),
		NumberBox.new(parent, Rect(140, (pid * 25), 60, 20))
			.value_(currSFU[\keys][pid][5])
			.action_({|val| // "DUR VAL: ".post; val.value.postln;
				~crps.updateSoundFileUnit(currfile, pid, nil, nil, val.value.ceil);
				~crps.segmentUnits(currfile);
				~updateSFViews.value(currfile, top, bot, pid);
//				~updateCurrentSoundFileUnit.value(pid,1);
				~gui[\soundFileUnitRowViews][pid][5].value_((currSFU[\keys][pid][0..]++currSFU[\mfccs][pid][0..]).asString);
			}),
		TextField.new(parent, Rect(205, (pid * 25), 700, 20))
			.value_((currSFU[\keys][pid][0..]++currSFU[\mfccs][pid][0..]).asString),
		pid.asInteger ];
};

~updateSoundFileUnitRow = { |rid, bot|
	[rid, bot].postln;
	~gui[\soundFileUnitRowViews][rid][3].value_(~crps[\sfutable][~gui[\cf]][\keys][rid][4]); ////!!!!!!!!!
	~gui[\soundFileUnitRowViews][rid][4].value_(~crps[\sfutable][~gui[\cf]][\keys][rid][5]);
	bot.setSelection(rid, [0, 0]);
	bot.setSelection(rid, ~crps[\sfutable][~gui[\cf]][\keys][rid][4..5] * 44.1);
	//"Row ".post; rid.post; " updated.".postln;
};

~updateSFViews = { |cfpath, top, bot, currid|
	var theunits = ~crps[\sfutable][cfpath][\keys];
	theunits.postln;
	// clear Views
	(0..63).do({ |i| top.selectNone(i) });
	bot.selectNone(0);
	// read out units into the two views if they exist
	(theunits != nil).if
	{
		theunits.do({ |row, index| top.setSelection(index, [row[4], row[5]] * 44.1) });
		bot.setSelection(0, (theunits[currid][4..5] * 44.1));
		//unselect
		Post << "CURR: " << ~gui[\cp] << " ... " << "NEXT: " << currid << "\n";
		(~gui[\soundFileUnitRowViews][~gui[\cp]] != nil).if { ~gui[\soundFileUnitRowViews][~gui[\cp]][0].value_(0)};
		//update
		~gui[\cp] = currid;
		~gui[\soundFileUnitRowViews][~gui[\cp]][0].value_(1);
		//~updateCurrentSoundFileUnit.value(currid,1);
	};
};

~updateMFCCSpectrum = { |currfile, pid, tab|
	var currSFU = ~crps[\sfutable][currfile], foo;

	currSFU[\mfccs][pid][6..].ampdb.postln;

	(~gui[\tabs][tab][\areas][\highlighter][\view].children != nil).if { ~gui[\tabs][tab][\areas][\highlighter][\view].children.do({ |child| child.remove; child.free }) };
	~gui[\tabs][tab][\areas][\highlighter].add(\ms -> MultiSliderView.new(~gui[\tabs][tab][\areas][\highlighter][\view], Rect(0,0,125,225)));

	currSFU[\mfccs][pid][6..].postln; "::::".postln;
	foo = (1 + (currSFU[\mfccs][pid][6..].ampdb / 32)).min(1.0).max(0.0);
	//foo.postln;

	~gui[\tabs][tab][\areas][\highlighter][\ms].value_(foo);
	~gui[\tabs][tab][\areas][\highlighter][\ms].isFilled_(true); // width in pixels of each stick


	~gui[\tabs][tab][\areas][\highlighter][\ms].indexThumbSize_(225 / 25 * 0.2); // spacing on the value axis
	~gui[\tabs][tab][\areas][\highlighter][\ms].gap_((225 / 25 * 0.8));
	~gui[\tabs][tab][\areas][\highlighter][\ms].indexIsHorizontal_(false);
	~gui[\tabs][tab][\areas][\highlighter][\view].refresh;


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

	range = ~crps.removeSoundFileUnit(~gui[\cf], did);
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
			top.setSelection(prow, ~crps[\sfutable][~gui[\cf]][\keys][prow][4..5] * 44.1);
		});
		bot.setSelection(0, ~crps[\sfutable][~gui[\cf]][\keys][~gui[\cp]][4..5] * 44.1);
	};
	~crps.segmentUnits(~gui[\cf]);
};

~buildGraphParams = { |tabnum, xx, yy, ss, dragtarget = nil, fontcolor, initSelection|



	var descrids = Array[], selected;
	var graph = ~gui[\tabs][tabnum][\areas][\graph], graphparams = ~gui[\tabs][tabnum][\areas][\graphParams];
	var gpheight = graphparams[\view].bounds.height;

	~crps[\dtable].keys.asArray.sort.do({ |descr| descrids = descrids.add(~crps[\dtable][descr].asString) });
	~gui[\tabs][tabnum][\xx] = ~gui[\tabs][tabnum][\xx] ? xx;
	~gui[\tabs][tabnum][\yy] = ~gui[\tabs][tabnum][\yy] ? yy;
	~gui[\tabs][tabnum][\ss] = ~gui[\tabs][tabnum][\ss] ? ss;

	~gui[\tabs][tabnum][\areas][\graphParams][\view].children.do({|child| child.remove; child.free });
	"BUILDING GRAPH PARAMS!!!".postln;

	graph.sync;
	graph.descriptors_(xx, yy, ss).fontColor_(fontcolor).dragTarget_(dragtarget).zoomFactor_(1@1).transFactor_(0@0);
	graph.clients_([	// unitData Viewer, player, MFCC plotter
		{ |chosen1| graphparams[\unitData].items_(~crps.getSoundFileUnitMetadata(chosen1.state[0], chosen1.state[1])[0..20].collect({|elem| elem.asString})) },	// link scene to metadata view
		{ |chosen2| ~playSegment.value(chosen2.state) },
		{ |chosen3|
			~updateMFCCSpectrum.value(~crps[\sfmap][chosen3.state[0]], chosen3.state[1], 2);
//			Plotter(parent: ~gui[\tabs][tabnum][\areas][\highlighter])
//				.value_(~crps[\sfutable][ ~crps[\sfmap][chosen3.state[0]] ][\mfccs][chosen3.state[1]][6..].ampdb)
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


			.items_(~crps[\dtable].keys.asArray.sort.collect({ |descr| ~crps[\dtable][descr].asString })))
		.add(\xLabel -> StaticText(graphparams[\view], Rect(85, 5, 80, 20))
			.string_("X").stringColor_(Color.green))



		.add(\minList -> ListView(graphparams[\view], Rect(85, 25, 80, gpheight - 25))
			.stringColor_(Color.white).selectedStringColor_(Color.green)
			.items_(~gui[\tabs][tabnum][\areas][\graph].cArray[0..20].collect({|col| col.minItem.asString }))
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
			.items_(~gui[\tabs][2][\areas][\graph].cArray.flop[0][0..20].collect({|item| item.asString }))
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
			.items_(~gui[\tabs][tabnum][\areas][\graph].cArray[0..20].collect({|col| col.maxItem.asString }))
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




~playSegment = { |pair|
	var cf, relid = pair[1];
	(pair[0].isNumber).if { cf = ~crps[\sfmap][pair[0]] } { cf = pair[0] };
	"Playing unit number: ".post; relid.post; ", from ".post; cf.postln;
	Post << "offset: " << (~crps[\sfutable][cf][\keys][relid][4] * 0.001) << " dur: " << (~crps[\sfutable][cf][\keys][relid][5] * 0.001) << Char.nl;
	(~crps[\sftrees][ cf ].tree[\bfrR] == nil).if
	{
		"PLAYING MONO".postln;
		~audio[\monoPlayer].set(\bufNum, ~crps[\sftrees][ cf ].tree[\bfrL].bufnum, \offset, (~crps[\sfutable][cf][\keys][relid][4] * 0.001), \dur, (~crps[\sfutable][cf][\keys][relid][5] * 0.001));	// msecs -> secs
		~audio[\mBus].set(1);


	} {



		"PLAYING STEREO".postln;
		~audio[\stereoPlayer].set(\bufNumL, ~crps[\sftrees][ cf ].tree[\bfrL].bufnum, \bufNumR, ~crps[\sftrees][ cf ].tree[\bfrR].bufnum, \offset, (~crps[\sfutable][cf][\keys][relid][4] * 0.001), \dur, (~crps[\sfutable][cf][\keys][relid][5] * 0.001));	// msecs -> secs
		~audio[\sBus].set(1);
	}
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
(
// 7. CHANGE THE FIRST ARGUMENT TO ANY STRING YOU LIKE


~crps = CorpusDB.new("CORPUS.GUI", s);

~audio.add(\mBus -> Bus.control(s, 1));
~audio.add(\monoPlayer -> Synth.new(\unitSamplerMono, [\busTrig, ~audio[\mBus]]));
~audio.add(\sBus -> Bus.control(s, 1));
~audio.add(\stereoPlayer -> Synth.new(\unitSamplerStereo, [\busTrig, ~audio[\sBus]]));



////***********************************************************************************************
////		Begin GUI code
////
//// window + top level container (tabbedView) + top level container views and storage

~gui = Dictionary[];
~gui.add(\topWindow -> Window((~crps[\anchor].asString ++ " db"), Rect(305, 75, 1120, 840)).acceptsMouseOver_(true));
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
	~gui[\tabs].add(ix -> Dictionary[\nameTag -> [\import, \analyze, \graph][ix], \view -> ScrollView(~gui[\tabs][\view].views[ix], ~gui[\tabs][\view].views[ix].asView.bounds), \areas -> Dictionary[], \needsUpdate -> true])
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
		.gridColor_(Color.gray)


		.action_())
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
			zoomArray = ~crps[\sfutable][~gui[\cf]][\rawmels].flop;
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
			~crps.updateSoundFileUnit(~gui[\cf], ~gui[\cp], nil,
				(~gui[\tabs][1][\areas][\waveform][\closeView].selectionStart(0) / 44.1).max(0).floor,
				(~gui[\tabs][1][\areas][\waveform][\closeView].selectionSize(0) / 44.1).ceil);

			// update number boxes + fullView
			~updateSoundFileUnitRow.value(~gui[\cp], ~gui[\tabs][1][\areas][\waveform][\fullView]);

			~crps.segmentUnits(~gui[\cf]);


			~gui[\soundFileUnitRowViews].keys.do({ |pid|
				~gui[\soundFileUnitRowViews][pid][5].value_((~crps[\sfutable][ ~gui[\cf] ][\keys][pid][0..]++~crps[\sfutable][ ~gui[\cf] ][\mfccs][pid][0..]).asString); // $$$$
			});
			~updateMFCCSpectrum.value(~gui[\cf], ~gui[\cp], 2);


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
						~gui[\latestSFID] = ~crps.addSoundFile(thepath.fullPath, sf.numChannels);
						fileListView.items_((fileListView.items ++ [thepath.fileName]).asSet.asArray.sort);
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
						var counter, fileListView, sf, thepath, thefiles, theentries, candidatePath, mdFlag;
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
									candidatePath = thefiles[counter - 1].fullPath;
									mdFlag = ".md.".matchRegexp(candidatePath.asString);
									Post << "PATH: " << candidatePath << "... MDFLAG: " << mdFlag << "\n";
									(mdFlag == false).if
									{
										~gui[\latestSFID] = ~crps.addSoundFile(candidatePath, sf.numChannels);
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
		//"/Users/tms/dev/supercollider/2.manifold/snd/chanting.speaking/73356_voice_prayer_speaking_reverberant.md.aiff".matchRegexp("md")
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
					~crps[\sftrees].keys.do({|filename|
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
	.add(\fileListView -> ListView(~gui[\tabs][0][\areas][\filesList][\view], Rect(5, 30, 400, 400))
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
			cf = ~gui[\fileListDict][ entry ];

			fileListArea[\fileListView].items_(fileListArea[\fileListView].items);
			fileListArea[\fileListView].refresh;

			//fileListView.items_((fileListView.items ++ [thepath.fileName]).asSet.asArray.sort);

			addedFileListArea[\addedFileListView].items_((addedFileListArea[\addedFileListView].items ++ [entry]).asSet.asArray.sort);
			~gui[\tabs][1][\areas][\buttons][\corpusFiles].items_(~gui[\tabs][1][\areas][\buttons][\corpusFiles].items ++ [entry]);


			~gui[\cf] = cf;
			// if non-nil, read out cached units into sfunitrows $$$$
			"units... ".postln;
			(~crps[\sfutable][~gui[\cf]][\keys] != nil).if
			{
				" from cache...".postln;
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

				(~crps[\sfutable][cf][\rawmels] == nil).if
				{	// stream it
					{~crps.analyzeSoundFile(cf, sfid:~gui[\latestSFID], verbose:true)}.fork(SystemClock);
					// schedule the updates
					{
						while({
							(~crps[\sftrees][cf].tree[\bfrL] == nil);
						},{
							~crps[\sftrees][cf].tree[\bfrL].postln;
							0.1.wait;
						});
						0.01.wait;
						analysisArea.children.do(_.remove);

//						~origArray = ~crps[\sfutable][~gui[\cf]][\rawmels];
//						~modArray = [];
//						~descriptors = [0,1,2,7].sort;
//						~origArray.flop.do({ |col,index| (~descriptors.asSet.includes(index)).if { ~modArray = ~modArray ++ col.bubble } });
//						//~modArray.postcs;

						Plotter(parent: analysisArea)
							.value_(~crps[\sfutable][~gui[\cf]][\rawmels].flop) //(~modArray)  //[0]
							.plotMode_(\plines)


							.superpose_(false)
							.findSpecs_(true)


							.editFunc_({|pl, pli, idx, val|


								pli.post; "...".post; idx.post; "....".post; val.postln;
								~crps[\sfutable][cf][\rawmels][pli][idx] = val;
							});


						fileListArea[\fileListView].enabled = true;
					}.fork(AppClock);

				} { // post it if cached
					analysisArea.children.do(_.remove);
					Plotter(parent: analysisArea)
						.value_(~crps[\sfutable][cf][\rawmels].flop)  //[0]
						.plotMode_(\plines)


						.superpose_(false)


						.findSpecs_(true)


						.editFunc_({|pl, pli, idx, val|
							pli.post; "...".post; idx.post; "....".post; val.postln;
							~crps[\sfutable][cf][\rawmels][pli][idx] = val;


						});
					AppClock.sched(0, { |tm| fileListArea[\fileListView].enabled = true });
				};
			};
			// update the sfviews (close and full) no matter what
			~updateSFViews.value(~gui[\cf], waveformArea[\fullView], waveformArea[\closeView], 0);
		}));




~gui[\tabs][0][\areas][\addedFilesList].add(\addedFileListView -> ListView(~gui[\tabs][0][\areas][\addedFilesList][\view], Rect(5, 30, 400, 400))


	.stringColor_(Color.white)
	.items_([]));




//// segments area
~gui[\tabs][1][\areas][\segBtns].add(\plusButton -> Button(~gui[\tabs][1][\areas][\segBtns][\view], Rect(2, 5, 20, 20))


	.states_([["+",Color.black,Color.white]])
	.value_(0)
	.mouseUpAction_({ |bttn|
		var np, tmp;
		var waveformArea = ~gui[\tabs][1][\areas][\waveform];
		(~crps[\sfutable][~gui[\cf]][\keys] == nil).if
		{
			np = 0;
			~gui[\latestSFID] = ~crps.addSoundFileUnit(~gui[\cf], np, [0, 500]);
		} { // else
			tmp = ~crps[\sfutable][~gui[\cf]][\keys].collect({ |unit| unit[4..5] }).sort({|a,b| a[0] > b[0]});
			np = ~crps[\sfutable][~gui[\cf]][\keys].size;
			~crps.addSoundFileUnit(~gui[\cf], np, [tmp[0][0] + tmp[0][1], 500]);
		};

		~crps.segmentUnits(~gui[\cf]);
		tmp = ~newSoundFileUnitRow.value(~gui[\cf], np, ~gui[\tabs][1][\areas][\segments][\view], waveformArea[\fullView], waveformArea[\closeView]);
		~gui[\soundFileUnitRowViews] = ~gui[\soundFileUnitRowViews].add(np -> tmp);
		waveformArea[\fullView].setSelection(np, ~crps[\sfutable][~gui[\cf]][\keys][np][4..5] * 44.1);
		~scheduleUpdates.value(1);

		(~crps[\sfutable][~gui[\cf]][\keys].size == 1).if { ~gui[\soundFileUnitRowViews][0][0].value_(0); ~gui[\soundFileUnitRowViews][0][0].valueAction_(1) };

	});
);
~gui[\tabs][1][\areas][\segBtns].add(\fillButton -> Button(~gui[\tabs][1][\areas][\segBtns][\view], Rect(2, 30, 20, 20))
	.states_([["->",Color.black,Color.white]])
	.value_(0)
	.mouseUpAction_({ |bttn|
		var last = ~gui[\soundFileUnitRowViews].size - 1;
		var sfile = ~gui[\cf];
		var sfid = ~crps[\sfmap].findKeyForValue(~gui[\cf]);
		var waveformArea = ~gui[\tabs][1][\areas][\waveform];
		// update the last one, filling to duration of sfile
		~crps.updateSoundFileUnit(sfile, last,
			dur: (~crps[\sftrees][~gui[\cf]].tree[\bfrL].duration * 1000) - ~crps.getSoundFileUnitMetadata(sfid , last)[4]);
		(last > 0).if
		{
			((last - 1)..0).do({ |pid|
				~crps.updateSoundFileUnit(sfile, pid,
					dur: (~crps.getSoundFileUnitMetadata(sfid , pid + 1)[4] - ~crps.getSoundFileUnitMetadata(sfid , pid)[4]));
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
			(~crps[\sfutable][~gui[\cf]][\keys] != nil).if
			{
				" from cache...".postln;
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

				(~crps[\sfutable][cf][\rawmels] == nil).if
				{	// stream it
					{~crps.analyzeSoundFile(cf, sfid:~gui[\latestSFID], verbose:true)}.fork(SystemClock);
					// schedule the updates
					{
						while({
							(~crps[\sftrees][cf].tree[\bfrL] == nil);
						},{
							~crps[\sftrees][cf].tree[\bfrL].postln;
							0.1.wait;
						});
						0.01.wait;
						analysisArea.children.do(_.remove);

//						~origArray = ~crps[\sfutable][~gui[\cf]][\rawmels];
//						~modArray = [];
//						~descriptors = [0,1,2,7].sort;
//						~origArray.flop.do({ |col,index| (~descriptors.asSet.includes(index)).if { ~modArray = ~modArray ++ col.bubble } });
//						//~modArray.postcs;

						Plotter(parent: analysisArea)
							.value_(~crps[\sfutable][~gui[\cf]][\rawmels].flop) //(~modArray)  //[0]
							.plotMode_(\plines)


							.superpose_(false)
							.findSpecs_(true)


							.editFunc_({|pl, pli, idx, val|


								pli.post; "...".post; idx.post; "....".post; val.postln;
								~crps[\sfutable][cf][\rawmels][pli][idx] = val;
							});


						fileListArea[\fileListView].enabled = true;
					}.fork(AppClock);

				} { // post it if cached
					"RAW DESCRIPTORS ARE NOT NIL!".postln;
					analysisArea.children.do(_.remove);
					Plotter(parent: analysisArea)
						.value_(~crps[\sfutable][cf][\rawmels].flop)  //[0]
						.plotMode_(\plines)


						.superpose_(false)


						.findSpecs_(true)


						.editFunc_({|pl, pli, idx, val|
							pli.post; "...".post; idx.post; "....".post; val.postln;
							~crps[\sfutable][cf][\rawmels][pli][idx] = val;


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
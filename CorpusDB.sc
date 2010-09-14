CorpusDB : Dictionary {

	var <>sfOffset, <>cuOffset, <>pFactor, <>soundFileUnitsMapped;

	*new { |name, server|
		^super.new.initCorpus(name, server)
	}
	
	initCorpus { |cid, srvr|
		// anchor is an identifier for a corpus (a name, a path, whatever)
		this.put(\anchor, cid.asSymbol);
		// link to a server (never stored)
		this.put(\server, srvr);
		// the dictionaries that store the data/metadata
		this.put(\sftable, Dictionary[]);		// sfile and buffer info
		this.put(\sfmap, Dictionary[]);			// map sfile indexes (as they appear in pu- and cutable) to sfile pathes (as in sftable)
		this.put(\sfutable, Dictionary[]);		// provisional units metadata table
		this.put(\cutable, Dictionary[]);		// corpus units metadata table
		// keep track of the latest additions to the tables
		this.sfOffset = 0;		this.cuOffset = 0;
		// dtable = descriptors table; must match to analysis synth
		this.put(\dtable, Dictionary[0 -> \unitID, 1 -> \sfileID, 2 -> \sfRelID, 3 -> \onset, 4 -> \duration, 5 -> \tartiniFreq, 6 -> \tartiniHasFreq, 7 -> \power, 8 -> \flatness, 9 -> \centroid, 10 -> \zerox, 11 -> \flux, 12 -> \rolloff, 13 -> \slope, 14 -> \spread, 15 -> \crest]);
		// send the analysis synth to the server
		this.perThreshold_(0.6);
		this.buildAnalysisSynth(this.pFactor);
		// init the cached flag to false (since there are no units to cache yet
		this.soundFileUnitsMapped = false;
		^this
	}

	perThreshold_ { |pf|
		this.pFactor = pf;
		this.buildAnalysisSynth(this.pFactor);
		^this
	}

	buildAnalysisSynth { |pfact=0.6, res=25, reportInterval=10|
		this.put( \ansynthdef,
			SynthDef(\analyzer, { |srcbufNum, start, dur, savebufNum, inTrig|
				var trig, env, in, chain, freq, hasFreq, power, flatness, centroid, zerox, flux, rolloff, slope, spread, crest, mfcc, line, driver, driver2;
				trig = InTrig.kr(inTrig);
				env = EnvGen.kr(Env.linen(0.01, (dur - 0.02), 0.01, 1), gate: trig, doneAction: 2);
				in = PlayBuf.ar(1, srcbufNum, BufRateScale.kr(srcbufNum), startPos: start, trigger: trig) * env;
				chain = FFT(LocalBuf(2048,1), in);
			
				# freq, hasFreq = 	Tartini.kr(in, pfact);       // Seems to be better than pitch.kr
				power =			FFTPower.kr(chain);          // empirical multiplier
				flatness =		SpecFlatness.kr(chain);
				centroid =		SpecCentroid.kr(chain);
				zerox =			ZeroCrossing.ar(in);
				flux =			FFTFlux.kr(chain, 0.9);      //spectral flux...averaged over a segment or subsegment?
				rolloff = 		SpecPcile.kr(chain, 0.9);    // rolloff @ 90%
				//rumble = 		FFTRumble.kr(chain);         // have to measure f0 first!
				slope = 			FFTSlope.kr(chain);
				spread =			FFTSpread.kr(chain);
				crest =			FFTCrest.kr(chain);
				
				mfcc =			MFCC.kr(chain);

				// gather all the metadata for each analysis window into a list
				line = [freq, hasFreq, power.ampdb.max(-120), (10 * flatness.log).max(-120), centroid, (zerox / 2048), (flux * 100), rolloff, (slope * 10000), (spread * 0.000001), crest, mfcc].flatten;
				// log the metadata into a buffer and signal sclang to read from the buffer
				driver = Impulse.kr( 44100 / 1024 );
				Logger.kr( line, driver, savebufNum );
				SendReply.kr( driver, \frame, line, 25 );
				// signal sclang to redraw the analysis data at regular intervals
				driver2 = reportInterval / BufDur.kr(srcbufNum);
				SendTrig.kr( Impulse.kr(driver2), reportInterval, savebufNum ); 
				// play the sound that is being analyzed
				Out.ar(0, in);
			}));
		this[\ansynthdef].send(this[\server]);	// must kill old synthdef if nec. !!!!???
	}
	
	addSoundFile { |path, numChannels=1, uniqueFlag=nil|     // TODO: Add filtering to ensure that just sound files are added!
		var thepath = PathName.new(path.asString), flag;
		Post << "Adding Entry:   ===============================  " << path.asString;
		Post << " (" << numChannels << " channels)." << Char.nl;
		(this[\sftable][thepath.fullPath] == nil).if   // don't add if the entry already exists
		{
			(uniqueFlag == nil).if { flag = (Date.getDate.rawSeconds - 110376000) } { flag = uniqueFlag };  // allow a custom unique id to be passed in
			// create the sftable entry
			this[\sftable].add(thepath.fullPath -> 
					Dictionary[\abfr -> nil, \uniqueid -> flag, \channels -> numChannels]); // the nils are dummy keys (reminders)
			// read the sound file into a buffer and store a reference to that buffer in the DB
			this[\sfutable].add(thepath.fullPath -> Dictionary[\mfccs -> nil, \units -> nil]);
			
			(numChannels == 1).if
			{
				Buffer.readChannel(this[\server], thepath.fullPath, 0, -1, [0], { |bfr| 
					this[\sftable][thepath.fullPath].add(\bfrL -> bfr);
					// start the prov. unit table entry for this file
				});
			};
			(numChannels == 2).if
			{
				Buffer.readChannel(this[\server], thepath.fullPath, 0, -1, [0], { |bfr| 
					this[\sftable][thepath.fullPath].add(\bfrL -> bfr);				});
				Buffer.readChannel(this[\server], thepath.fullPath, 0, -1, [1], { |bfr| 
					this[\sftable][thepath.fullPath].add(\bfrR -> bfr);
				});
			};
			^thepath.fullPath		// returns the path
		};
		^nil
	}
	
	removeSoundFile { |path| // TODO: AND ASSOCIATED METADATA!!!!!
		var thepath = PathName.new(path.asString);
		"Deleting Entry:   ===============================".postln;
		thepath.postln;
		(this[\sftable][thepath.fullPath] != nil).if
		{
			this[\sftable][thepath.fullPath][\bfrL].free;
			(this[\sftable][thepath.fullPath][\bfrR] != nil).if { this[\sftable][thepath.fullPath][\bfrR].free };
			this[\sftable][thepath.fullPath][\abfr].free;
			this[\sftable][thepath.fullPath].add(\abfr -> nil, \bfrL -> nil, \bfrR -> nil, \uniqueid -> nil);
			this[\sftable].add(thepath.fullPath -> nil);
		} {
			"Something has gone horribly wrong; attempting to remove a non-existant path!".postln;
		};
//		this[\sftable][thepath.fullPath].postln;
	}
	
	analyzeSoundFile { |path, mapFlag=nil, invertFlag=false|
		var pbuf, abuf, thesynth, mapping;
		// the 'play' buffer for the analysis:
		(invertFlag == false).if { pbuf = this[\sftable][path][\bfrL] } { pbuf = this[\sftable][path][\bfrR] };
		"Analyzing: ".post; pbuf.bufnum.post; " with duration: ".post; pbuf.duration.postln;

		(this[\responder] != nil).if { this[\responder].remove };
		this.add(\responder -> OSCresponder(this[\server].addr,'/frame', { |time,resp,msg|
			//msg.postln;
			this.addRawMetadata(path, msg[3..13].asArray, msg[14..].asArray);
		}));
		//this[\responder].postln;
		this[\responder].add;
		this.add(\analysisTrigger -> Bus.control(this[\server], 1));

		// allocate a buffer for the analysis metadata, and fire off the analysis synth
		abuf = Buffer.alloc(this[\server], (pbuf.duration * (( 44100 / 1024 ) * 1.01)).ceil, 11, //25.25
		{ |an|
			//an.bufnum.postln;
			Synth(this[\ansynthdef].name.asSymbol, 
				[\srcbufNum, pbuf.bufnum,
				\start, 0, \dur, (pbuf.duration * 1.01),
				\savebufNum, an.bufnum,
				\inTrig, this[\analysisTrigger]]);
			this[\sftable][path.asString][\abfr] = an;   // store a reference to the synth [is this necessary?]
			this[\sfutable][path].add(\rawdescrs -> Array[], \rawmels -> Array[]);
			this[\analysisTrigger].set(1);
		});	// the synth will release itself
		this.mapIDToSF(path, mapFlag);					   // store the index -> sfile path mapping
		^this[\sftable][path.asString][\abfr]
	}
	
	mapIDToSF { |path, customMap=nil|
		var mapping;
		(customMap == nil).if
			{
				mapping = this.sfOffset.asInteger;
				this.sfOffset = this.sfOffset + 1;
			} {
				mapping = customMap;
				this.sfOffset = mapping.max(this.sfOffset) + 1;
			};
		this[\sfmap].add(mapping -> path);
	}

	addSoundFileUnit { |path, relid, bounds, cid=nil|
		var trio;
		(bounds != nil).if
		{
			"Adding sound file unit (to sfutable)... ".post;
			trio = [cid ? this.cuOffset, this[\sfmap].findKeyForValue(path.asString), relid ];
			(cid == nil).if { this.cuOffset = this.cuOffset + 1 } {this.cuOffset = this.cuOffset.max(cid) + 1 };
			Post << trio << " ... " << path << Char.nl;
			this[\sfutable][path][\units] = this[\sfutable][path][\units] ++ 
				[trio ++ bounds].flatten.reject({|item| item == nil}).clump(5); // reject is probably unnecessary
			this[\sfutable][path][\mfccs] = this[\sfutable][path][\mfccs] ++ 
				[trio ++ bounds].flatten.reject({|item| item == nil}).clump(5);
			this.soundFileUnitsMapped = false;
//			this[\sfutable][path][\units].size.postln;
			^this[\sfutable][path][\units].size
		};
	}
	
	updateSoundFileUnit { |path, relid, cid=nil, onset=nil, dur=nil, md=nil, mfccs=nil|
		(cid != nil).if
		{
			this[\sfutable][path][\units][relid] = ([cid] ++
				this[\sfutable][path][\units][relid] = this[\sfutable][path][\units][relid][1..]).flatten;
			this[\sfutable][path][\mfccs][relid] = ([cid] ++
				this[\sfutable][path][\mfccs][relid] = this[\sfutable][path][\mfccs][relid][1..]).flatten;
		};
		(onset != nil).if
		{
			this[\sfutable][path][\units][relid] = (this[\sfutable][path][\units][relid][0..2] ++ onset ++
				this[\sfutable][path][\units][relid][4..]).flatten;
			this[\sfutable][path][\mfccs][relid] = (this[\sfutable][path][\mfccs][relid][0..2] ++ onset ++
				this[\sfutable][path][\mfccs][relid][4..]).flatten;
		};
		(dur != nil).if
		{
			this[\sfutable][path][\units][relid] = (this[\sfutable][path][\units][relid][0..3] ++ dur ++
				this[\sfutable][path][\units][relid][4..]).flatten;
			this[\sfutable][path][\mfccs][relid] = (this[\sfutable][path][\mfccs][relid][0..3] ++ dur ++
				this[\sfutable][path][\mfccs][relid][4..]).flatten;
		};
		(md != nil).if { this[\sfutable][path][\units][relid] = this[\sfutable][path][\units][relid][0..4] ++ md };
		(mfccs != nil).if { this[\sfutable][path][\mfccs][relid] = this[\sfutable][path][\mfccs][relid][0..4] ++ mfccs };
	}

	removeSoundFileUnit { |path, relid|
		(relid != nil).if
		{
			this[\sfutable][path][\units].removeAt(relid);
			this[\sfutable][path][\mfccs].removeAt(relid);
			(relid == this[\sfutable][path][\units].size).if
			{
				^nil
			} {
				(relid..(this[\sfutable][path][\units].size - 1)).do({ |i|
					this[\sfutable][path][\units][i][2] = i;
					this[\sfutable][path][\mfccs][i][2] = i;
				});
				this.soundFileUnitsMapped = false;
				^(relid..(this[\sfutable][path][\units].size - 1))
			};
		};
	}

	clearSoundFileUnits { |path|		
		this[\sfutable][path].add(\units -> nil, \mfccs -> nil);
		this.soundFileUnitsMapped = false;
	}
	
	addRawMetadata { |path, descriptors=nil, mels=nil|
		var descr = descriptors, last = this.pFactor;
		(descr != nil).if
		{
			(descr[1] == 0).if { descr[1] = last } { last = descr[1] };
		};
		this[\sfutable][path][\rawdescrs] = this[\sfutable][path][\rawdescrs].add(descriptors);
		this[\sfutable][path][\rawmels] = this[\sfutable][path][\rawmels].add(mels);
	}
	
	// raw analysis data -> segmented metadata (desriptors + mfccs)
	segmentUnits { |path|
		var descrs, mfccs;
//		"Analyze units path:".post; path.postln;
		descrs = this[\sfutable][path][\rawdescrs].flop[0..10];
		mfccs = this[\sfutable][path][\rawmels].flop;
		this[\sfutable][path][\units].do({ |cell, indx|
			var low, high, len, ra = Array[], rba = Array[];
			low = (cell[3] / 40).floor.asInteger;
			len = (cell[4] / 40).ceil.asInteger;
			high = low + len;
			descrs.do({ |row, ix| ra = ra.add(row[low..high].mean.asStringPrec(4).asFloat) });
			mfccs.do({ |row, ix| rba = rba.add(row[low..high].mean.asStringPrec(6).asFloat) });
			this[\sfutable][path][\units][indx] = this[\sfutable][path][\units][indx][0..4].add(ra).flatten;
			this[\sfutable][path][\mfccs][indx] = this[\sfutable][path][\mfccs][indx][0..4].add(rba).flatten;
		});
	}
	
	addCorpusUnit { |uid, metadata|
		this[\cutable].add(uid -> metadata);
		^this[\cutable][uid]
	}
	
	removeCorpusUnit { |uid|
		this[\cutable].removeAt(uid);
		this.soundFileUnitsMapped = false;
		^this[\cutable][uid]
	}
	
	clearCorpusUnits {
		// dereference them all
		this[\cutable].keysDo({ |cid| this[\cutable].add(cid -> nil) });
	}
	
	getSoundFileUnitMetadata { |sfid, uid|
		(this.soundFileUnitsMapped != true).if
		{
			this.mapSoundFileUnitsToCorpusUnits;
		};
		^this[\cutable].detect({ |item, i| ((item[1] == sfid) && (item[2] == uid)) });
	}	

	mapSoundFileUnitsToCorpusUnits {
		(this.soundFileUnitsMapped == false).if
		{
			this.clearCorpusUnits;
			this[\sfutable].do({ |path|
				path[\units].do({ |pu, index|
					this.addCorpusUnit(pu[0], (pu ++ path[\mfccs][index][5..]).flatten); 
				});
			});
			this.soundFileUnitsMapped = true;
		};
		^this[\cutable]
	}

	importCorpusFromXML { |server, path|
		var domdoc, tmpDict = Dictionary[], dDict = Dictionary[], mDict = Dictionary[]; //tmpMap = Dictionary[], 
		
		"Adding File Entry from XML:   ===============".postln;
		path.postln;
		
		this.initCorpus;
		this.sfOffset = 0; this.cuOffset = 0;
		
		domdoc = DOMDocument.new(path);
		domdoc.getDocumentElement.getElementsByTagName("descrid").do({ |tag, index|
			tmpDict.add(tag.getText.asInteger -> tag.getAttribute("name").asSymbol);
		});
		(tmpDict != this[\dtable]).if { "You fucked up.".postln; };
		
		domdoc.getDocumentElement.getElementsByTagName("sfile").do({ |tag, index|
			var theID = tag.getElementsByTagName("id")[0].getText.asInteger;
			var theName = tag.getAttribute("name").asString;
			this.addSoundFile(theName, tag.getElementsByTagName("uniqueid")[0].getText.asFloat);
			this.mapIDToSF(theName, theID);
		});
		
//		"THE MAP:".postln;
//		this[\sfmap].postln;
		
		domdoc.getDocumentElement.getElementsByTagName("punit").do({ |tag, index|
			var tmpRow = tag.getText.split($ ).asFloat;
			postln("descr: " ++ tmpRow[0] ++ " -> " ++ tmpRow);
			dDict.add(tmpRow[0] -> tmpRow);
		});
		domdoc.getDocumentElement.getElementsByTagName("munit").do({ |tag, index|
			var tmpRow = tag.getText.split($ ).asFloat;
			postln("mfcc: " ++ tmpRow[0] ++ " -> " ++ tmpRow);
			mDict.add(tmpRow[0]-> tmpRow);
		});
		
//		dDict.keys.asArray.sort.postln;
//		mDict.keys.asArray.sort.postln;
//		
//		"=================".postln;
		
		dDict.keys.asArray.sort.do({ |key|
			var descr = dDict[key], mfcc = mDict[key], path = this[\sfmap][descr[1]].asString, last;
//			Post << "Path: " << path << Char.nl;
//			mfcc.postln;
//			[path, descr[2], descr[3..4], descr[0]].postln;
			last = this.addSoundFileUnit(path, descr[2], descr[3..4], descr[0]) - 1;
//			Post << "Last \units row: " << this[\sfutable][path] << Char.nl;
			this[\sfutable][path][\units][last] = (this[\sfutable][path][\units][last] ++ descr[5..]).flatten;
			this[\sfutable][path][\mfccs][last] = (this[\sfutable][path][\mfccs][last] ++ mfcc[5..]).flatten;
			
		});
		this.soundFileUnitsMapped = false;
	}

	exportCorpusToXML { |server, path|
		File.use(path.asString, "w", { |f|
			f.write("<?xml version=1.0 encoding=iso-8859-1 standalone=yes?>\n");
			f.write("<corpusmap name=\"" ++ this[\anchor].asString ++ "\">\n");
			f.write("    <heading name=\"DMAP\">\n");
			this[\dtable].keys.asArray.sort.do({|ky|
				f.write("	        <descrid name=\"" ++ this[\dtable][ky].asString ++ "\">" ++ ky.asString ++ "</descrid>\n");
			});
			f.write("    </heading>\n");
			f.write("    <heading name=\"FMETA\">\n");
			this[\sftable].keysValuesDo({|sfpath, entry|
				var sfile = SoundFile.new;
				((sfpath.isInteger != true) && (this[\sfmap].findKeyForValue(entry[\bfrL].path.asString).asString != nil)).if
				{
				// entry[\bfrL].path.postln;
					sfile.openRead(entry[\bfrL].path.asString);
					f.write("        <sfile name=\"" ++ entry[\bfrL].path.asString ++ "\">\n");
					f.write("            <id>" ++ this[\sfmap].findKeyForValue(entry[\bfrL].path.asString).asString ++ "</id>\n");
					f.write("            <uniqueid>" ++ entry[\uniqueid].asString ++ "</uniqueid>\n");
					f.write("            <duration>" ++ (entry[\bfrL].numFrames / entry[\bfrL].sampleRate) ++ "</duration>\n");
					f.write("            <numchannels>" ++ entry[\bfrL].numChannels ++ "</numchannels>\n");
					f.write("            <sr>" ++ entry[\bfrL].sampleRate ++ "</sr>\n");
					f.write("            <samptype>" ++ sfile.sampleFormat ++ "</samptype>\n");
				
					f.write("        </sfile>\n");
				};
			});
			f.write("    </heading>\n");
			f.write("    <heading name=\"UNITS\">\n");
			this[\sfutable].do({|upath|
				upath[\units].do({ |row, num|
					f.write("        <punit sfid=\"" ++ row[1].asString ++ "\" relid=\"" ++ row[2].asString ++ "\">" ++ row.join($ ).asString ++ "</punit>\n");
					f.write("        <munit sfid=\"" ++ row[1].asString ++ "\" relid=\"" ++ row[2].asString ++ "\">" ++ upath[\mfccs][num].join($ ).asString ++ "</munit>\n");
				});

			});
			
			f.write("    </heading>\n");
			f.write("</corpusmap>\n");
			
		});
	}
}
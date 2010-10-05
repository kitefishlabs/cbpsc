CorpusDB : Dictionary {

	var <>sfOffset, <>cuOffset, <>sfgOffset, <>pFactor, <>soundFileUnitsMapped;

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
		this.put(\sfgmap, Dictionary[]);
		this.put(\sfutable, Dictionary[]);		// provisional units metadata table
		this.put(\cutable, Dictionary[]);		// corpus units metadata table
		// keep track of the latest additions to the tables
		this.sfOffset = 0;		this.cuOffset = 0;		this.sfgOffset = 0;
		// dtable = descriptors table; must match to analysis synth
		this.put(\dtable, Dictionary[0 -> \unitID, 1 -> \sfgrpID, 2 -> \sfileID, 3 -> \sfRelID, 4 -> \onset, 5 -> \duration, 6 -> \tartiniFreq, 7 -> \tartiniHasFreq, 8 -> \power, 9 -> \flatness, 10 -> \centroid, 11 -> \zerox, 12 -> \flux, 13 -> \rolloff, 14 -> \slope, 15 -> \spread, 16 -> \crest]);
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
	
	addSoundFile { |path, numChannels=1, uniqueFlag=nil, sfGrpID=nil|     // TODO: Add filtering to ensure that just sound files are added?
		var thepath = PathName.new(path.asString), flag;
		Post << "Adding Entry:   ===============================  " << path.asString;
		Post << " (" << numChannels << " channels)." << Char.nl;
		(this[\sftable][thepath.fullPath] == nil).if   // don't add if the entry already exists
		{
			// allow a custom unique id to be passed in
			(uniqueFlag == nil).if { flag = (Date.getDate.rawSeconds - 110376000) } { flag = uniqueFlag };
			// create the sftable entry
			this[\sftable].add(thepath.fullPath -> 
					Dictionary[\abfr -> nil, \uniqueid -> flag, \channels -> numChannels, \sfilegroup -> (sfGrpID ? 0)]); // the nils are dummy keys (reminders)
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
			this[\sftable][thepath.fullPath].add(\abfr -> nil, \bfrL -> nil, \bfrR -> nil, \uniqueid -> nil, \sfilegroup -> nil);
			this[\sftable].add(thepath.fullPath -> nil);
		} {
			"Something has gone horribly wrong; attempting to remove a non-existant path!".postln;
		};
//		this[\sftable][thepath.fullPath].postln;
	}
	
	analyzeSoundFile { |path, mapFlag=nil, invertFlag=false, group=0|
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
		this.mapIDToSF(path, group, mapFlag);
		// store the index -> sfile path mapping
		^this[\sftable][path.asString][\abfr]
	}
	
	mapIDToSF { |path, sfgroup=0, customMap=nil|
		var mapping;
		(customMap == nil).if
			{
				mapping = this.sfOffset.asInteger;
				this.sfOffset = this.sfOffset + 1;
			} {
				mapping = customMap;
				// custom caller responsible for sfOffset!!!
				//this.sfOffset = mapping.max(this.sfOffset) + 1;
			};
		// no check for overwrite... should there be one?
		(this[\sfgmap][sfgroup] == nil).if { this[\sfgmap].add(sfgroup -> Array[])};
		this[\sfgmap][sfgroup] = (this[\sfgmap][sfgroup] ++ mapping).flatten;
		this[\sfmap].add(mapping -> path);
		this[\sftable][path].add(\sfilegroup -> sfgroup);
	}

	addSoundFileUnit { |path, relid, bounds, cid=nil, sfg=nil|
		var quad;
		(bounds != nil).if
		{
			Post << "Adding sound file unit (to sfutable)...mapping: "; // << this[\sfmap].findKeyForValue(path.asString);
			quad = [cid ? this.cuOffset, sfg ? this.sfgOffset, this[\sfmap].findKeyForValue(path.asString), relid ];
			// custom caller responsible!
			(cid == nil).if { this.cuOffset = this.cuOffset + 1 }; // {this.cuOffset = this.cuOffset.max(cid) + 1 };
			Post << quad << " ... " << path << Char.nl;
			this[\sfutable][path][\units] = this[\sfutable][path][\units] ++ 
				[quad ++ bounds].flatten.reject({|item| item == nil}).clump(6); // reject is probably unnecessary
			this[\sfutable][path][\mfccs] = this[\sfutable][path][\mfccs] ++ 
				[quad ++ bounds].flatten.reject({|item| item == nil}).clump(6);
			this.soundFileUnitsMapped = false;
//			this[\sfutable][path][\units].size.postln;
			^this[\sfutable][path][\units].size
		};
	}

	updateSoundFileUnit { |path, relid, cid=nil, onset=nil, dur=nil, md=nil, mfccs=nil, sfg=nil|
		var old = this[\sfutable][path][\units][relid];
		var temp = [cid ? old[0], sfg ? old[1], old[2], old[3], onset ? old[4], dur ? old[5]];
		var newmd = md ? old[6..];
		var newmfccs = mfccs ? this[\sfutable][path][\mfccs][relid][6..];
		
		this[\sfutable][path][\units][relid] = temp ++ newmd;
		this[\sfutable][path][\mfccs][relid] = temp ++ newmfccs;
		
		this[\sfutable][path][\units][relid].postln;
		this[\sfutable][path][\mfccs][relid].postln;
		
		(sfg != nil).if { this[\sftable][path].add(\sfilegroup -> sfg) };
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

	// set the \units and \mfccs tables to nil (empty them) for the provided path
	clearSoundFileUnits { |path|		
		this[\sfutable][path].add(\units -> nil, \mfccs -> nil);
		this.soundFileUnitsMapped = false;
	}

	addRawMetadata { |path, descriptors=nil, mels=nil|
		var descr = descriptors, last = this.pFactor;
		(descr != nil).if
		{
			// if tartiniHasFreq == 0, sub in pFactor...the min. allowed periodicity (this skews the median values less, perhaps?)
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
			low = (cell[4] / 40).floor.asInteger;
			len = (cell[5] / 40).ceil.asInteger;
			high = low + len;
			descrs.do({ |row, ix| ra = ra.add(row[low..high].mean.asStringPrec(4).asFloat) });
			mfccs.do({ |row, ix| rba = rba.add(row[low..high].mean.asStringPrec(6).asFloat) });
			this[\sfutable][path][\units][indx] = this[\sfutable][path][\units][indx][0..5].add(ra).flatten;
			this[\sfutable][path][\mfccs][indx] = this[\sfutable][path][\mfccs][indx][0..5].add(rba).flatten;
		});
	}

	// add a uid -> metadata mapping to the \cutable (should there be a check to see that uid == metadata[0]?
	addCorpusUnit { |uid, metadata|
		this[\cutable].add(uid -> metadata);
		^this[\cutable][uid]
	}

	// opposite of add; sets the mapped flag to false (why?)
	removeCorpusUnit { |uid|
		this[\cutable].removeAt(uid);
		this.soundFileUnitsMapped = false;
		^this[\cutable][uid]
	}

	// dereference them all
	clearCorpusUnits {
		this[\cutable].keysDo({ |cid| this[\cutable].add(cid -> nil) });
	}

	getSoundFileUnitMetadata { |sfid, uid|
		(this.soundFileUnitsMapped != true).if
		{
			this.mapSoundFileUnitsToCorpusUnits;
		};
		^this[\cutable].detect({ |item, i| ((item[2] == sfid) && (item[3] == uid)) });
	}	

	// map
	mapSoundFileUnitsToCorpusUnits {
		(this.soundFileUnitsMapped == false).if
		{
			this.clearCorpusUnits;
			this[\sfutable].do({ |path|
				path[\units].do({ |pu, index|
					//[pu[0], (pu ++ path[\mfccs][index][6..]).flatten].postln;
					this.addCorpusUnit(pu[0], (pu ++ path[\mfccs][index][6..]).flatten); 
				});
			});
			this.soundFileUnitsMapped = true;
		};
		^this[\cutable]
	}

// import & export entire corpora
	importCorpusFromXML { |server, path|
		var domdoc, tmpDict = Dictionary[], dDict = Dictionary[], mDict = Dictionary[];
		var sfgroupentries, sfentries, runningCUOffset = 0, runningSFOffset = 0, runningSFGOffset = 0;
		
		"Adding File Entry from XML: ".post;
		path.postln;
		"=============".postln;
		
//		this.initCorpus(path, server);
		Post << "Starting from sf offset: " << this.sfOffset << " + cu Offset: " << this.cuOffset << " + sfg Offset: " << this.sfgOffset << Char.nl;
		
		domdoc = DOMDocument.new(path);
		domdoc.getDocumentElement.getElementsByTagName("descrid").do({ |tag, index|
			tmpDict.add(tag.getText.asInteger -> tag.getAttribute("name").asSymbol);
		});
		(tmpDict != this[\dtable]).if { "Import descriptor list mismatch!".postln; };

		sfentries = domdoc.getDocumentElement.getElementsByTagName("sfile").do({ |entry|
			var theName = entry.getAttribute("name").asString;
			var theID = entry.getElementsByTagName("id")[0].getText.asInteger;
			var theGroup = entry.getElementsByTagName("group")[0].getText.asInteger;
			["...",entry].postln;
			this.addSoundFile(theName,
				entry.getElementsByTagName("numchannels")[0].getText.asInteger,
				entry.getElementsByTagName("uniqueid")[0].getText.asFloat,
				(theGroup + this.sfgOffset)
			);
			Post << "MAPPING!: " << theName << " + " << (theGroup + this.sfgOffset) << " + " << theID << Char.nl;
			this.mapIDToSF(theName, (theGroup + this.sfgOffset), (theID + this.sfOffset));
			runningSFOffset = runningSFOffset.max(theID);
			Post << "runningSFOffset after a sfile entry iteration: " << runningSFOffset << Char.nl;
			runningSFGOffset = runningSFGOffset.max(theGroup);
			Post << "runningSFGOffset after a sfgroup entry iteration: " << runningSFGOffset << Char.nl;
		});
		
//	});		
		
//		"THE MAP:".postln;
//		this[\sfmap].postln;
		
		domdoc.getDocumentElement.getElementsByTagName("corpusunit").do({ |tag, index|
			var tmpRow = tag.getText.split($ ).asFloat;
//			Post << "descr: " << tmpRow[0] << " -> " << tmpRow << Char.nl;
			(dDict[tmpRow[1]] == nil).if
			{
				dDict.add(tmpRow[1] -> Dictionary[tmpRow[0] -> tmpRow]);
			} {
				dDict[tmpRow[1]].add(tmpRow[0] -> tmpRow);
			};				
		});


		dDict.keys.asArray.sort.do({ |sfg| // sfilegroup keys
			var innerDict = dDict[sfg], path, tmp, last;
			
			innerDict.keys.asArray.sort.do({ |key|
				tmp = innerDict[key];
				path = this[\sfmap][tmp[2]].asString;
//				Post << "path: " << path << Char.nl;
				last = this.addSoundFileUnit(path, tmp[3].asInteger, tmp[4..5], cid: (tmp[0].asInteger + this.cuOffset), sfg: (sfg + this.sfgOffset).asInteger) - 1;
				this[\sfutable][path][\units][last] = (this[\sfutable][path][\units][last] ++ innerDict[key][6..15]).flatten;
				this[\sfutable][path][\mfccs][last] = (this[\sfutable][path][\mfccs][last] ++ innerDict[key][16..]).flatten;
				
				runningCUOffset = runningCUOffset.max(tmp[0].asInteger);
				
			});			
		});
		
		// update cuOffset, sfOffset, sfgOffset;
		this.cuOffset = this.cuOffset + runningCUOffset + 1;
		this.sfOffset = this.sfOffset + runningSFOffset + 1;
		this.sfgOffset = this.sfgOffset + runningSFGOffset + 1;
		Post << "After import: " << this.cuOffset << " + " << this.sfOffset << " + " << this.sfgOffset << Char.nl;

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
					f.write("            <group>" ++ entry[\sfilegroup].asString ++ "</group>\n");
					f.write("            <id>" ++ this[\sfmap].findKeyForValue(entry[\bfrL].path.asString).asString ++ "</id>\n");
					f.write("            <uniqueid>" ++ entry[\uniqueid].asString ++ "</uniqueid>\n");
					f.write("            <duration>" ++ (entry[\bfrL].numFrames / entry[\bfrL].sampleRate) ++ "</duration>\n");
					((entry[\bfrL] != nil) && (entry[\bfrR] != nil)).if
					{
						f.write("            <numchannels>" ++ 2 ++ "</numchannels>\n");
					} {
						f.write("            <numchannels>" ++ 1 ++ "</numchannels>\n");
					};
					f.write("            <sr>" ++ entry[\bfrL].sampleRate ++ "</sr>\n");
					f.write("            <samptype>" ++ sfile.sampleFormat ++ "</samptype>\n");
					f.write("        </sfile>\n");
				};
			});
			f.write("    </heading>\n");
			f.write("    <heading name=\"UNITS\">\n");
			this.mapSoundFileUnitsToCorpusUnits;
			this[\cutable].keys.asArray.sort.do({ |cid|
				var drow = this[\cutable][cid];
				f.write("        <corpusunit sfid=\"" ++ drow[2].asString ++ "\" relid=\"" ++ drow[3].asString ++ "\">" ++ this[\cutable][cid].join($ ).asString ++ "\"</corpusunit>\n");
			});
			f.write("    </heading>\n");
			f.write("</corpusmap>\n");
			
		});
	}
}
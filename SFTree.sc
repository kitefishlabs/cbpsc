//This file is part of cbpsc (last revision @ version 1.0).
//
//cbpsc is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
//
//cbpsc is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License along with cbpsc.  If not, see <http://www.gnu.org/licenses/>.
//
// cbpsc : created by Tom Stoll : tms@kitefishlabs.com : www.kitefishlabs.com
//
// SFTree.sc
// Copyright (C) 2013, Thomas Stoll

SFTree {
	var <>corpus, <>anchorPath, <>nodes, <>sfMap, <>procMap, <>procMapOffset;

	*new { |corpus, anchorpath, verbose=nil|
		^super.new.initSFTree(corpus, anchorpath, verbose)
	}

	initSFTree { |corpus, anchorpath, verbose|
		this.corpus = corpus;
		this.anchorPath = anchorpath;
		this.nodes = Dictionary[];
		this.sfMap = Dictionary[];
		this.procMap = Dictionary[];
		this.procMapOffset = 0;
		^this
	}

	checkProcMap { |hashstring|
		(this.procMap[hashstring].isNil).if {
			// this.procMap.add(hashstring -> this.procMapOffset);
			this.procMap.add(this.procMapOffset -> hashstring);
			this.procMapOffset = this.procMapOffset + 1;
			^this.procMap[hashstring]
		} {
			// ^this.procMap[hashstring]
			^this.procMap[(this.procMapOffset - 1)]
		};
	}

	addRootNode { |filename, sfID, tRatio, sndSubdir=nil, uniqueFlag=nil, verbose=nil|

		var relPath, joinedPath, uniqueflag, sndFile, duration, chnls, synthdef, procID;

		(sndSubdir.isNil).if {
			relPath = filename;
			joinedPath = this.anchorPath +/+ "snd" +/+ filename;
		} {
			relPath = sndSubdir +/+ filename;
			joinedPath = this.anchorPath +/+ "snd" +/+ sndSubdir +/+ filename;
		};
		uniqueflag = uniqueFlag ? 1000000.rand;

		sndFile = SoundFile.new; sndFile.openRead(joinedPath); sndFile.close;

		duration = sndFile.duration;
		Post << "dur: " << duration << "\n";
		chnls = sndFile.numChannels;

		synthdef = (chnls == 1).if { "monoSamplerNRT" } { "stereoSamplerNRT" };
		this.nodes.add(sfID -> SamplerNode.new(relPath, synthdef, duration, uniqueflag, chnls, tRatio, sfID));
		Post << "sfID: " << sfID << "hashstring: " << this.nodes[sfID].hashstring << "\n";
		procID = this.checkProcMap(this.nodes[sfID].hashstring);
		this.nodes[sfID].postln;
		this.sfMap.add(sfID -> [relPath, duration, tRatio, synthdef, procID]);
		^this.nodes[sfID]
	}

	addChildNode { |parentID, childID, tRatio, synthdef, params, uniqueFlag=nil|

		var uniqueflag, parentNode, procID;

		uniqueflag = uniqueFlag ? 1000000.rand;
		(this.nodes[parentID].isNil).if {
			Post << "Parent (" << parentID << ") does not exist. Cannot create child (" << childID << ").\n";
			^nil;
		} {
			parentNode = this.nodes[parentID];
		};

		this.nodes.add(childID -> EfxNode.new(synthdef, params, parentNode.duration, uniqueflag, parentNode.channels, parentNode.tRatio, childID, parentID));
		this.nodes[parentID].postln;
		procID = this.checkProcMap(this.nodes[childID].hashstring);
		this.sfMap.add(childID -> [synthdef, params, procID]);
		^this.nodes[this.nodes[childID].sfID]
	}
}


SFNode {
	var <>synth, <>params, <>duration, <>uniqueID, <>channels, <>tRatio, <>sfID, <>hashstring, <>unitSegments, <>unitAmps, <>unitMFCCs;

	*new { |synthname, params=nil, duration= -1, uniqueID= -1, channels=1, tRatio=1.0, sfID= -1, verbose=nil|
		^super.new.initSFNode(synthname, params, duration, uniqueID, channels, tRatio, sfID, verbose)
	}

	initSFNode { |synthname, params, duration, uniqueID, channels, tRatio, sfID, verbose|

		this.synth = synthname;
		this.params = params;
		this.duration = duration;
		this.uniqueID = uniqueID;
		this.channels = channels;
		this.tRatio = tRatio;
		this.sfID = sfID;
		this.hashstring = "";
		this.unitSegments = List[]; // create an empty container for unit bounds and tags
		this.unitAmps = Dictionary[];
		this.unitMFCCs = Dictionary[];
	}

	addOnsetAndDurPair { |onset, duration, relID=nil|

		// Post << "add onset: " << onset << " and duration: " << duration << "\n";

		relID = (relID.isNil).if { this.unitSegments.size } { relID };

		// Post << "rel. ID is: " << relID << "\n";

		(this.unitSegments[relID].isNil).if {
			this.unitSegments = this.unitSegments ++ [ SFU.new(0.max(onset).min((this.duration / this.tRatio))) ];
		} {
			this.unitSegments[relID] =  0.max(onset).min(this.duration);
		};
		this.unitSegments[relID].duration = ((this.duration / this.tRatio) - this.unitSegments[relID].onset).min(0.max(duration));
		// Post << this.unitSegments[relID] << "\n";
		^this.unitSegments[relID]
	}

	updateUnitSegmentParams{ |relID, onset=nil, duration=nil, tag=nil|

		(onset.isNil.not).if { this.unitSegments[relID].onset = onset } { };
		(duration.isNil.not).if { this.unitSegments[relID].duration = duration } { };
		(tag.isNil.not).if { this.unitSegments[relID].tag = tag } { };
		^this.unitSegments[relID]
	}

	calcRemainingDur { ^this.duration - this.unitSegments.last.onset }

	sortSegmentsList { this.unitSegments = this.unitSegments.sort({ |a,b| a.onset < b.onset }); ^this.unitSegments }

	addMetadataForRelID { |relID, amps, mfccs|

		((relID.isNil.not) && (amps.isNil.not)).if { this.unitAmps.add(relID -> amps) } { }; // in ML format
		((relID.isNil.not) && (mfccs.isNil.not)).if { this.unitMFCCs.add(relID -> mfccs) } { }; // in ML format

	}
}




SamplerNode : SFNode {
	var <>sfPath, <>buffer;

	*new { |sfpath, synthname, duration= -1, uniqueID= -1, channels=1, tRatio=1.0, sfID= -1, verbose=nil|
		^super.new.initSFNode(synthname, nil, duration, uniqueID, channels, tRatio, sfID).initSamplerNode(sfpath)
	}

	initSamplerNode { |sfpath|
		"Assigning sfPath to SamplerNode!".postln;
		this.sfPath = sfpath;
		this.buffer = nil;
		this.hashstring = this.synth ++ this.tRatio.asFloat.asStringPrec(5);
		^this
	}

	mapBuffer { |buffer|

		this.buffer = buffer;
		^this.buffer

	}

	jsonRepr {
		^Dictionary["relpath" -> this.sfPath,
			"synth" -> this.synth,
			"params" -> this.params,
			"duration" -> this.duration,
			"uniqueID" -> this.uniqueID,
			"channels" -> this.channels,
			"tRatio" -> this.tRatio,
			"sfID" -> this.sfID,
			"hash" -> this.hashstring]
	}
}


EfxNode : SFNode {
	var <>parentID;

	*new { |synthname, params, duration= -1, uniqueID= -1, channels=1, tRatio=1.0, childID= -1, parentID= -1, verbose=nil|
		^super.new.initSFNode(synthname, params, duration, uniqueID, channels, tRatio, childID).initEfxNode(parentID)
	}

	initEfxNode { |parentID|
		this.parentID = parentID;
		this.hashstring = "";
		this.params.pairsDo({ |a,b|
			((Set['inbus', 'outbus', 'dur', 'envDur', 'transp'] & [a].asSet).size == 0).if {
				this.hashstring = this.hashstring ++ a.asString ++ b.asString;
			};
		});

		^this
	}

	jsonRepr {
		^Dictionary["parentID" -> this.parentID,
			"synth" -> this.synth,
			"params" -> this.params,
			"duration" -> this.duration,
			"uniqueID" -> this.uniqueID,
			"channels" -> this.channels,
			"tRatio" -> this.tRatio,
			"sfID" -> this.sfID,
			"hash" -> this.hashstring]
	}
}

SFU {
	var <>onset, <>duration, <>tag;

	*new { |onset=0, duration=0, tag=0, verbose=nil|
		^super.new.initSFU(onset, duration, tag)
	}

	initSFU { |onset, duration, tag|
		this.onset = onset;
		this.duration = duration;
		this.tag = tag;
		^this
	}
}
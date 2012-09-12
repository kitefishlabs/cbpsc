//This file is part of cbpsc (last revision @ version 1.0).
//
//cbpsc is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
//
//cbpsc is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License along with cbpsc.  If not, see <http://www.gnu.org/licenses/>.
//
// used in cbpsc : created by Thomas Stoll : tms@kitefishlabs.com : www.corpora-sonorus.com
//
// CorpusSoundFileTree.sc
// Copyright (C) 2011-2012, Thomas Stoll

CorpusSoundFileTree {
	
	var <>corpus, <>anchorPath, <>tree, <>trackbacks, <>indexPool;
	
	*new { |corpus|
		^super.new.initCorpusSoundFileTree(corpus)
	}
	
	initCorpusSoundFileTree { |crps|
		this.corpus = crps;
		this.tree = Dictionary[];
		this.trackbacks = Dictionary[];
		^this
	}
	
//	setAnchorSFTree { |path, numChannels=1, uniqueFlag=nil, sfGrpID=nil, srcFileID=nil, synthdefs=nil, params=nil, tratio=1, sfg=0|
//		var flag, sfID;
//		
//		this.anchorPath = PathName.new(path.asString).fullPath;
//
//		// set and correct (if nec.) the new sfID
//		(srcFileID == nil).if
//		{ 
//			sfID = this.corpus.sfOffset;
//			this.corpus.sfOffset = this.corpus.sfOffset + 1;
//		} {
//			sfID = this.corpus.sfOffset.max(srcFileID);
//			this.corpus.sfOffset = sfID + 1;
//		};
//
//		(uniqueFlag == nil).if { flag = (Date.getDate.rawSeconds - 110376000) } { flag = uniqueFlag };
//
//		this.tree = Dictionary[
//			sfID ->
//				Dictionary[
//					\abfr -> nil,
//					\bfrL -> nil,
//					\bfrR -> nil,
//					\uniqueID -> flag,
//					\channels -> numChannels,
//					\sfileGroup -> (sfGrpID ? 0),
//
//					\sfileID -> sfID,
//					\parentFileID -> sfID,
//					\synthdefs -> synthdefs,
//					\params -> params,
//					\tratio -> tratio,
//					
//					\children -> Dictionary[]
//				]
//		];
//		this.corpus.mapIDToSF(anchorPath, customMap: sfID, sfgrp:sfg);
//		Post << "Creating trackback for: " << sfID << "\n";
//		this.trackbacks = Dictionary[(sfID -> [anchorPath, synthdefs, params, tratio])];
//		^sfID
//	}
	
// - addAnchorSFTree { |path, numChannels=1, uniqueFlag=nil, sfGrpID=nil, srcFileID=nil, synthdefs=nil, params=nil, tratio=1, sfg=0, verbose=nil|
// path, 
// numChannels=1, 
// uniqueFlag=nil, 
// sfg=0
// srcFileID=nil, 
// synthdefs=nil, 
// params=nil
// tratio=1


	addAnchorSFTree { |path, numChannels=1, uniqueFlag=nil, sfg=nil, srcFileID=nil, synthdefs=nil, params=nil, tratio=1, verbose=nil|
		var flag, sfID;
		(verbose != nil).if { Post << "Add an anchor tree...\n"; };
		this.anchorPath = PathName.new(path.asString).fullPath;

		// set and correct (if nec.) the new sfID
		(srcFileID == nil).if
		{ 
			sfID = this.corpus.sfOffset;
			this.corpus.sfOffset = this.corpus.sfOffset + 1;
		} {
			sfID = this.corpus.sfOffset.max(srcFileID);
			this.corpus.sfOffset = sfID + 1;
		};

		// SUN March 13, 2011 ca. 3AM == 1.3 billion seconds since epoch
		(uniqueFlag == nil).if { flag = (Date.getDate.rawSeconds - 1300000000) } { flag = uniqueFlag };

		"sdif: ".post; sfID.postln;

//		this.tree.add(
//			sfID ->
//				Dictionary[
		this.tree.add(\abfr -> nil); // just as a reminder
		this.tree.add(\bfrL -> nil);
		this.tree.add(\bfrR -> nil);
		this.tree.add(\uniqueID -> flag);
		this.tree.add(\channels -> numChannels);
		this.tree.add(\sfileGroup -> sfg);

		this.tree.add(\sfileID -> sfID);
		this.tree.add(\parentFileID -> sfID);
		this.tree.add(\synthdefs -> synthdefs);
		this.tree.add(\params -> params);
		this.tree.add(\tratio -> tratio);
					
		this.tree.add(\children -> Dictionary[]);
//				]
//		);
		this.corpus.mapIDToSF(anchorPath, customMap:sfID, sfgrp:sfg);
		(verbose != nil).if { Post << "Creating trackback for: " << sfID << "\n"; };
		this.trackbacks.add(sfID -> [anchorPath, synthdefs, params, tratio]);
		^sfID
	}
	
// - addChildSFTree { |sourceFileID=nil, synthdef=nil, params=nil, tratio=1, sfg=0, verbose=nil|
// sourceFileID=nil	assume the worst
// synthdef=nil
// params=nil
// tratio=1
// sfg=0				0 is the default, rather than nil

	addChildSFTree { |sourceFileID=nil, numChannels=1, synthdef=nil, params=nil, tratio=1, sfg=0, verbose=nil|
		var travTree, travAccum = [], sfID, srcFileID;
		var parentSynthdefs, parentParams, psdPlusInsert, ppPlusInsert;
		(verbose != nil).if { Post << "src file id: " << sourceFileID << "\n"; };
		// set and correct (if nec.) the new sfID
		(sourceFileID == nil).if
		{
			srcFileID = this.trackbacks.keys.asArray.minItem; // assume that we are appending to root node
			sfID = this.corpus.sfOffset;
			this.corpus.sfOffset = this.corpus.sfOffset + 1;
		} {
			srcFileID = sourceFileID;
			(verbose != nil).if { Post << this.corpus.sfOffset << "\n"; };
			sfID = this.corpus.sfOffset.max(srcFileID);
			this.corpus.sfOffset = sfID + 1;
		};
			
		(this.trackbacks.keys.includes(srcFileID)).if  // be sure that the src file ID is valid!
		{
			parentSynthdefs = this.trackbacks[srcFileID][1].deepCopy; 
			parentParams = this.trackbacks[srcFileID][2].deepCopy;
			psdPlusInsert = parentSynthdefs.insert(1, synthdef).flatten;
			ppPlusInsert = parentParams.insert(1, params);
						
			this.tree[\children].add(
				sfID ->
					Dictionary[
						\abfr -> nil,
						
						\sfileID -> sfID,
						\sfileGroup -> sfg,
						\parentFileID -> srcFileID,
						\synthdefs -> psdPlusInsert,
						\params -> ppPlusInsert,
						\channels -> numChannels,
						\tratio -> tratio,

						\children -> Dictionary[]
					]
			);
			
			this.trackbacks = this.trackbacks.add(sfID -> [anchorPath, psdPlusInsert, ppPlusInsert, tratio]);
			this.corpus.mapIDToSF(this.trackbacks[sfID][0], customMap: sfID, sfgrp:sfg);
			(verbose != nil).if {
				Post << "updated trackbacks list:\n";
				Post << this.trackbacks << "\n";
			};
			^sfID
		};
		^nil
	}
}
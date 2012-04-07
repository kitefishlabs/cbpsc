//This file is part of cbpsc (last revision @ version 0.6).
//
//cbpsc is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
//
//cbpsc is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License along with cbpsc.  If not, see <http://www.gnu.org/licenses/>.
//
// used in cbpsc : created by Thomas Stoll : tms@corpora-sonorus.com : www.corpora-sonorus.com
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
//		this.corpus.mapIDToSF(anchorPath, customMap: sfID, sfgroup:sfg);
//		Post << "Creating trackback for: " << sfID << "\n";
//		this.trackbacks = Dictionary[(sfID -> [anchorPath, synthdefs, params, tratio])];
//		^sfID
//	}
	
	
	addAnchorSFTree { |path, numChannels=1, uniqueFlag=nil, sfGrpID=nil, srcFileID=nil, synthdefs=nil, params=nil, tratio=1, sfg=0, verbose=nil|
		var flag, sfID;
		(verbose).if { Post << "Add an anchor tree...\n"; };
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

		this.tree.add(
			sfID ->
				Dictionary[
					\abfr -> nil,
					\bfrL -> nil,
					\bfrR -> nil,
					\uniqueID -> flag,
					\channels -> numChannels,
					\sfileGroup -> (sfGrpID ? 0),

					\sfileID -> sfID,
					\parentFileID -> sfID,
					\synthdefs -> synthdefs,
					\params -> params,
					\tratio -> tratio,
					
					\children -> Dictionary[]
				]
		);
		this.corpus.mapIDToSF(anchorPath, customMap:sfID, sfgroup:sfg);
		(verbose).if { Post << "Creating trackback for: " << sfID << "\n"; };
		this.trackbacks.add(sfID -> [anchorPath, synthdefs, params, tratio]);
		^sfID
	}
	
	
	
	addChildSFTree { |sourceFileID=nil, synthdef=nil, params=nil, tratio=1, sfg=0, verbose=nil|
		var travTree, travAccum = [], sfID, srcFileID;
		var parentSynthdefs, parentParams, psdPlusInsert, ppPlusInsert;
		(verbose).if { Post << "src file id: " << sourceFileID << "\n"; };
		// set and correct (if nec.) the new sfID
		(sourceFileID == nil).if
		{
			srcFileID = this.trackbacks.keys.asArray.minItem; // assume that we are appending to root node
			sfID = this.corpus.sfOffset;
			this.corpus.sfOffset = this.corpus.sfOffset + 1;
		} {
			srcFileID = sourceFileID;
			(verbose).if { Post << this.corpus.sfOffset << "\n"; };
			sfID = this.corpus.sfOffset.max(srcFileID);
			this.corpus.sfOffset = sfID + 1;
		};
			
		(this.trackbacks.keys.includes(srcFileID)).if  // be sure that the src file ID is valid!
		{
			
			parentSynthdefs = this.trackbacks[srcFileID][1].deepCopy; 
			parentParams = this.trackbacks[srcFileID][2].deepCopy;
			psdPlusInsert = parentSynthdefs.insert(1, synthdef).flatten;
			ppPlusInsert = parentParams.insert(1, params);
						
			this.tree[srcFileID][\children].add(
				sfID ->
					Dictionary[
						\abfr -> nil,
						
						\sfileID -> sfID,
						\parentFileID -> srcFileID,
						\synthdefs -> psdPlusInsert,
						\params -> ppPlusInsert,
						\tratio -> tratio,

						\children -> Dictionary[]
					]
			);
			
			this.trackbacks = this.trackbacks.add(sfID -> [anchorPath, psdPlusInsert, ppPlusInsert, tratio]);
			this.corpus.mapIDToSF(this.trackbacks[sfID][0], customMap: sfID, sfgroup:sfg);
			(verbose).if {
				Post << "updated trackbacks list:\n";
				Post << this.trackbacks << "\n";
			};
			^sfID
		};
		^nil
	}
}
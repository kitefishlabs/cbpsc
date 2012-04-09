//This file is part of cbpsc (new class @ version 0.4).
//
//cbpsc is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
//
//cbpsc is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License along with cbpsc.  If not, see <http://www.gnu.org/licenses/>.
//
// cbpsc : created by Tom Stoll : tms@kitefishlabs.com : www.corpora-sonorus.com
//
// PartitionedCorpusSearch.sc
// Copyright 2010-11, Thomas Stoll

PartitionedCorpusSearch : CorpusSearch {
	var <>partitionIndex, <>partedCArray, <>partedTree, <>partedStats, <>partedNormedTree;
	
	*new { |crps, partitionIndex|
		^super.new(crps).initPartedCSearch(partitionIndex)
	}
	
	initPartedCSearch { |partitionIndex|
		this.partitionIndex = partitionIndex; // superclass holds the corpus!
		Post << "corpus (from superclass): " << this.corpus.class << "\n";
		this.partedCArray = this.corpus.mapPartitionedSoundFileUnitsToCorpusUnits(true);
		this.cArray = this.corpus.mapSoundFileUnitsToCorpusUnits;
		Post << "parted corpus array: " << this.partedCArray.size << "\n";
		Post << "basis corpus array: " << this.cArray.size << "\n";
		^this
	}
	
	getPartedStats {
		this.partedStats = this.partedCArray.keys.asArray.sort.collect({ |row| 
			[this.partedCArray[row], this.cArray[row]]
		}).flop;
		
		this.partedStats = this.partedStats[0].collect({ |pVal,pIndex|
			(this.partedStats[0][pIndex] == this.partitionIndex).if { this.partedStats[1][pIndex] };
		}).flop.collect({ |col|
			var stdev, mean;
			col = col.reject({ |item| item == nil });
			mean = col.mean;
			stdev = col.inject(0, { |sum, cell| sum + ((cell - mean) ** 2) });
			[col.minItem, col.maxItem, col.maxItem - col.minItem, mean, (stdev / col.size).sqrt, col[(col.size / 2).floor]]
		});
		^this.partedStats
	}
	
	buildTree { |metadata, descriptors, normFlag=false, lastFlag=true|
		var reducedArray = Array[];
		(descriptors == nil).if { ^nil };
		(metadata == nil).if 
		{
			this.partedCArray.keys.asArray.sort.do({ |cid|
				var check = this.partedCArray[cid], mdata = this.cArray[cid];
//				Post << "Check: " << check << ", part Index: " << this.partitionIndex;
				(check == this.partitionIndex).if
				{
//					Post << " PASSED";
					reducedArray = reducedArray.add(descriptors.collect({ |descr| mdata[descr] }));
				};
//				Post << "\n";
			});
		} {
			reducedArray = metadata;
		};
		(normFlag == true).if
		{
			reducedArray.flop.postln;
			reducedArray = reducedArray.flop.collect({ |col, index|
				((lastFlag == true) && (index != (descriptors.size - 1))).if
				{
					col.normalize;
				} {
					col
				};
			}).flop;
//			"After: ".postln;
//			reducedArray.postln;
			this.partedNormedTree = KDTree(reducedArray, lastIsLabel: lastFlag);
			^this.partedNormedTree
		};
		this.partedTree = KDTree(reducedArray, lastIsLabel: lastFlag);
		^this.partedTree
	}
	
	findNearestInRadius { |target, radius|
		^this.partedNormedTree.radiusSearch(target, radius).collect({|found| [found.label, found.location]})
	}
	
	findNNearest { |target, radius, normFlag = false, number=1|
		var res;
		(normFlag == true).if
		{
			res = this.partedNormedTree.radiusSearch(target, radius).collect({|found| [found.label, found.location]})[0..(number - 1)];
			^res
		} {
			res = this.partedTree.radiusSearch(target, radius).collect({|found| [found.label, found.location]})[0..(number - 1)];
			res = res.flop;
			res[1] = res[1].collect({ |list| (list != nil).if { list[0..(number - 1)] } });
			res = res.flop;
			^res
		};
	}
}

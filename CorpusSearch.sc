//This file is part of cbpsc (updated @ version 0.7).
//
//cbpsc is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
//
//cbpsc is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License along with cbpsc.  If not, see <http://www.gnu.org/licenses/>.
//
// cbpsc : created by Tom Stoll : tms@kitefishlabs.com : www.corpora-sonorus.com
//
// CorpusSearch.sc
// Copyright 2011-12, Thomas Stoll

CorpusSearch : Dictionary {
	var <>corpus, <>cArray, <>tree, <>stats, <>normedTree;
	
	*new { |crps|
		^super.new.initCSearch(crps)
	}
	
	initCSearch { |crps|
		this.corpus = crps;
		^this
	}
	
	getStats {
		this.cArray = this.corpus.mapSoundFileUnitsToCorpusUnits;
		this.stats = this.cArray.keys.asArray.sort.collect({ |row| this.cArray[row] })
			.flop.collect({ |col|
				var stdev, mean = col.mean;
				stdev = col.inject(0, { |sum, cell| sum + ((cell - mean) ** 2) });
				[col.minItem, col.maxItem, col.maxItem - col.minItem, mean, (stdev / col.size).sqrt, col[(col.size / 2).floor]]
			});
		^this.stats
	}
	
	buildNormalizedTree { |metadata, descriptors|
		^this.buildTree(metadata, descriptors, true, true);
	}
	
	buildTree { |metadata, descriptors, normFlag=false, lastFlag=true|
		var map, reducedArray = Array[];
		(descriptors == nil).if { Post << "Cannot build tree with NIL descriptors:\n"; ^nil };
		(metadata == nil).if 
		{
			map = this.corpus.mapSoundFileUnitsToCorpusUnits;
			map.keys.asArray.sort.do({ |cid|
				var row = map[cid];
				reducedArray = reducedArray.add(descriptors.collect({ |descr| row[descr] }));
			});
		} {
			reducedArray = metadata;
		};
		(normFlag == true).if
		{
			//reducedArray.flop.postln;
			reducedArray = reducedArray.flop.collect({ |col, index|
//				Post << lastFlag << " ::: " << index << " <? " << (descriptors.size - 1) << "\n";
				((lastFlag == true) && (index < (descriptors.size - 1))).if
				{
//					Post << "%%%\n";
					col.normalize;
				} {
//					Post << "$$$\n";
					col
				};
			}).flop;
//			"After: ".postln;
//			Post << lastFlag << "\n";
			this.normedTree = KDTree(reducedArray, lastIsLabel: lastFlag);
			^this.normedTree
		};
		this.tree = KDTree(reducedArray, lastIsLabel: lastFlag);
		^this.tree
	}
	
	findNearestInRadius { |target, radius|
		^this.normedTree.radiusSearch(target, radius).collect({|found| [found.label, found.location]})
	}
	
	findNNearest { |target, radius, normFlag = true, number=1|
		var res;
		(normFlag == true).if
		{
			res = this.normedTree.radiusSearch(target, radius).collect({|found| [found.label, found.location]})[0..(number - 1)];
			^res
		} {
			res = this.tree.radiusSearch(target, radius).collect({|found| [found.label, found.location]})[0..(number - 1)];
			res = res.flop;
			res[1] = res[1].collect({ |list| (list != nil).if { list[0..(number - 1)] } });
			res = res.flop;
			^res
		};
	}
}



//	nearestN { |unitsArray, descrArray, weightsArray, range|
//		var usize = unitsArray.size; //, dsize = descrArray.size;
////		var d0 = descrArray[0], d1 = descrArray[1], d2 = descrArray[2];
//		var sArray = Array.fill2D(3, usize, 0), theorder;
//		var stats = Array.fill(dsize,0), weights = weightsArray;
//		searchHash = Dictionary[];
//
//		(0..(usize - 1)).do({ |n|
//			sArray[0][n] = [n, unitsArray[n][d0], unitsArray[n][d1], unitsArray[n][d2]];
//		});
//
//		theorder = sArray[0].order({ |a,b| a[1] < b[1] });
//		theorder.do({ |uid, i|
//			var lo = 0.max(i-range);
//			var hi = usize.min(i + range);
//			sArray[1][i] = [uid] ++ theorder[lo..hi];
//		});
//
//		sArray[0].flop[1..].do({ |col, i|
//			stats[i] = (col.maxItem - col.minItem);
//		});
//		
//		sArray[1].do({ |ranked, rindex|
//			var u0, base, tmp;
//			u0 = ranked[0];
//			base = sArray[0][u0][1..];	// basis uid, looked up in sArray
//			sArray[2][u0] = Array.fill((ranked.size - 1), 0);
//			ranked[1..].do({ |unit,slot|
//				sArray[2][u0][slot] = ((((sArray[0][unit][1..] - base) / stats) ** 2) * weights).sum.sqrt; // 3 dim. eucl. distances
//			});
//			tmp = sArray[1][rindex].copy;
//			sArray[2][u0].order.do({ |therank, ix|
//				sArray[1][rindex][(ix+1)] = [tmp[(therank+1)], sArray[2][u0][therank]]
//			});
//		});
//		sArray[1] = sArray[1].sort({|a,b| a[0] < b[0] });
//		sArray[1].do({ |entry,index|
//			searchHash.add(index.asInteger -> entry[1..].flop)
//		});
//		^searchHash
//	}
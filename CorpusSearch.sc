CorpusSearch : Dictionary {
	var <>corpus, <>tree, <>normedTree;
	
	*new { |crps|
		^super.new.initCSearch(crps)
	}
	
	initCSearch { |crps|
		this.corpus = crps;
	}
	
	buildTree { |metadata = nil, descriptors, flag=true|
		var map, reducedArray = Array[];
		(descriptors == nil).if { ^nil };
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
		reducedArray.postln;
		this.tree = KDTree(reducedArray, lastIsLabel: flag);
		^this.tree
	}
	
	buildNormalizedTree { |metadata, descriptors, flag=true|
		var map, reducedArray = Array[];
		(descriptors == nil).if { ^nil };
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
		reducedArray.flop.postln;
		reducedArray = reducedArray.flop.collect({ |row, index|
			((flag == true) && (index != (descriptors.size - 1))).if
			{
				row.normalize;
			} {
				row
			};
		}).flop;
		this.normedTree = KDTree(reducedArray, lastIsLabel: flag);
		^this.normedTree
	}
	
	findNearestInRadius { |target, radius|
		^this.normedTree.radiusSearch(target, radius).collect({|found| [found.label, found.location]})
	}
	
	findNNearest { |target, radius, number=1|
		^this.normedTree.radiusSearch(target, radius).collect({|found| [found.label, found.location]})[0..(number - 1)];
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
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
// PartitionedCorpusDB.sc
// (c) 2010-11, Thomas Stoll

PartitionedCorpusDB : CorpusDB { // ... : Dictionary

	var <>partitionDescriptor, <>partitions, <>partedCorpus, <>partMap, <>partitionedSoundFileUnitsMapped;

	*new { |name, server, partitioning|
		^super.new(name, server).initPartitionedCorpus(partitioning);
	}

	initPartitionedCorpus { |partitioning|
		this.partitionDescriptor = partitioning[0];
		this.partitions = partitioning[1];
		this.partedCorpus = Dictionary[];
		this.partMap = Dictionary[];
		this.updatePartitions;
		this.add(\psfumap -> Dictionary[]);
		this.partitionedSoundFileUnitsMapped = false;
	}

	updatePartitions {
		this.partitions.keysValuesDo({ |key,val| val.do({ |value| this.partMap.add(value -> key) }) });
//		this.partedCorpus.add(\map -> Dictionary[]);
//		this.partitions.keysDo({ |key| this.partedCorpus[\map].add(key -> this.partitions[key].asSet) });
	}

	getPartitionedSoundFileUnitMetadata { |part, sfid, rid|
		var res;
		(this.partitionedSoundFileUnitsMapped != true).if
		{
			this.mapPartitionedSoundFileUnitsToCorpusUnits;
		};
		(this.partitions[ part ] == nil).if { ^nil };
		
		res = this[\cutable].detect({ |item, i| ((item[2] == sfid) && (item[3] == rid)) });
		(this.partedCorpus[ res[0] ] == part).if
		{
			^res
		} { ^nil };
	}
	
	mapPartitionedSoundFileUnitsToCorpusUnits { |override=false|
		((this.partitionedSoundFileUnitsMapped == false) || (override == true)).if
		{
			this.clearCorpusUnits;
			this[\sfutable].do({ |path|
				path[\units].do({ |pu, index|
					//[pu[0], (pu ++ path[\mfccs][index][6..]).flatten].postln;
					//Post << "mapping index: " << index << "\n";
					this.addCorpusUnit(pu[0], (pu ++ path[\mfccs][index][6..] ++ path[\keys][index][6]).flatten);

					//[pu[0], this.partMap[ pu[this.partitionDescriptor] ], this.partedCorpus.keys.includes(pu[0])].postln;

					(this.partedCorpus.keys.includes(pu[0]) == false).if
					{
						this.partedCorpus.add(pu[0] -> this.partMap[ pu[this.partitionDescriptor] ]);
					};
				});
			});
			this.partitionedSoundFileUnitsMapped = true;
			^this.partedCorpus
		} { ^nil };
	}
	
	mapByPartitionedSFRelID { //???
		this.mapBySFRelID;
		this.mapPartitionedSoundFileUnitsToCorpusUnits;
		
		this[\sfumap].keys.asArray.sort.do({ |sfkey|
			this[\sfumap][sfkey].keys.asArray.sort.do({ |ridkey|
		
				var currentSFTree = this[\sfumap][sfkey];
				var firstFoundUnit = this.getSoundFileUnitMetadata(sfkey,ridkey)[0];
				var currentPartMapping = this.partedCorpus[ firstFoundUnit ];
				// assume that all the units that map to a sfkey map to the same partition!!!
				
				(this[\psfumap][currentPartMapping] == nil).if
				{
					//Post << "currentSFTree: " << currentSFTree << "\n";
					this[\psfumap].add(currentPartMapping -> Dictionary[sfkey -> currentSFTree]);
				} {
					//Post << "curr sfkey: " << sfkey << ", currentSFTree: " << currentSFTree << "\n";
					this[\psfumap][currentPartMapping].add(sfkey -> currentSFTree);
				};
			});
		});
		^this[\psfumap]
	}
}
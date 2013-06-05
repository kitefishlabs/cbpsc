// power_mfcc24BusAnalyzerNRT - analysis Synthdef for modular analysis, goes at tail of chain
(
// hop = 40 msec == 0.04 seconds == 1 / 25 fps == 25 Hz analysis frequency
SynthDef(\power_mfcc24BusAnalyzerNRT, { |inbus=20, savebufNum=0, hop=0.04|
	var in, chain, power, mfccs, driver, array;
	in = In.ar(inbus, 1);
	chain = FFT(LocalBuf(4096,1), in);

	power =			FFTPower.kr(chain);          // empirical multiplier
	mfccs =			MFCC.kr(chain, numcoeff:24); // or 13|24|42...

	// log the metadata into a buffer and signal sclang to read from the buffer
	driver = Impulse.kr( 1 / hop );
	Logger.kr([power, mfccs].flatten, driver, savebufNum);
	Out.ar(0, in);
}).load(s);


// \monoSamplerNRT
	SynthDef(\monoSamplerNRT, { |outbus=20, srcbufNum, start=0, dur=1, transp=1|
		var env, chain;
		env = EnvGen.kr(Env.linen(0.01, ((BufDur.kr(srcbufNum) / transp) - 0.02), 0.01, 1), gate: 1, doneAction: 2);
	Out.ar(outbus, Pan2.ar(PlayBuf.ar(1, srcbufNum, BufRateScale.kr(srcbufNum) * transp, startPos: (start * BufSampleRate.kr(srcbufNum))) * env));
	}).load(s);


// \monoSampler
	SynthDef(\monoSampler, { |outbus=20, srcbufNum, start=0, dur=1, transp=1, attack=0.01, release=0.01|
		var env, in, chain;
		env = EnvGen.kr(Env.linen(attack, (dur - (attack+release)), release, 1, curve:\sine), gate: 1, doneAction: 2);
		in = PlayBuf.ar(1, srcbufNum, BufRateScale.kr(srcbufNum) * transp, startPos: (start * BufSampleRate.kr(srcbufNum) * transp), loop:1);
	Out.ar(outbus, Pan2.ar(in*env));
	}).load(s);


// \stereoSamplerNRT
	SynthDef(\stereoSamplerNRT, { |outbus=20, srcbufNum, start=0, dur=1, transp=1|
		var env, chain;
		env = EnvGen.kr(Env.linen(0.01, ((BufDur.kr(srcbufNum) / transp) - 0.02), 0.01, 1), gate: 1, doneAction: 2);
		chain = PlayBuf.ar(2, srcbufNum, BufRateScale.kr(srcbufNum) * transp, startPos: (start * BufSampleRate.kr(srcbufNum))) * env;
		Out.ar(outbus, chain);
	}).load(s);


// \stereoSampler
	SynthDef(\stereoSampler, { |outbus=20, srcbufNum, start=0, dur=1, transp=1, attack=0.01, release=0.01|
		var env, chain;
		env = EnvGen.kr(Env.linen(attack, ((dur / transp) - (attack+release)), release, 1), gate: 1, doneAction: 2);
		chain = PlayBuf.ar(2, srcbufNum, BufRateScale.kr(srcbufNum) * transp, startPos: (start * BufSampleRate.kr(srcbufNum))) * env;
		Out.ar(outbus, chain);
	}).load(s);

)

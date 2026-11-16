# -*- coding: utf-8 -*-
"""生成两首原创轻音乐，并调用本机 ffmpeg 编码为 MP3。

这个脚本只在制作素材时使用，运行程序本身不依赖 Python。
"""
import math
import os
import subprocess
import tempfile
import wave

import numpy as np

SR = 44100
FFMPEG = r"C:\Program Files (x86)\Lenovo\LegionZone\2.0.27.7062\SEGamingAI\services\editor\ffmpeg.exe"
OUT_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "music")


def midi_hz(note):
    return 440.0 * (2.0 ** ((note - 69) / 12.0))


def env_adsr(n, attack, decay, sustain, release):
    a = max(1, int(attack * SR))
    d = max(1, int(decay * SR))
    r = max(1, int(release * SR))
    env = np.ones(n, dtype=np.float64)
    a = min(a, n)
    env[:a] = np.linspace(0.0, 1.0, a) ** 0.7
    d_end = min(n, a + d)
    if d_end > a:
        env[a:d_end] = np.linspace(1.0, sustain, d_end - a)
    env[d_end:] = sustain
    if r < n:
        env[-r:] *= np.linspace(1.0, 0.0, r) ** 1.3
    return env


def pad_note(freq, dur, amp=0.12, detune=0.0015):
    n = int(dur * SR)
    t = np.arange(n) / SR
    env = env_adsr(n, 0.9, 0.6, 0.78, 1.8)
    lfo = 0.55 + 0.45 * np.sin(2 * math.pi * 0.09 * t + 0.4)
    left = (np.sin(2 * math.pi * freq * (1 - detune) * t)
            + 0.35 * np.sin(2 * math.pi * freq * 2 * (1 - detune) * t + 0.8)
            + 0.16 * np.sin(2 * math.pi * freq * 3 * t + 1.7))
    right = (np.sin(2 * math.pi * freq * (1 + detune) * t + 0.5)
             + 0.35 * np.sin(2 * math.pi * freq * 2 * (1 + detune) * t + 1.3)
             + 0.16 * np.sin(2 * math.pi * freq * 3 * t + 2.2))
    return left * env * lfo * amp, right * env * lfo * amp


def pluck(freq, dur, amp=0.2, bright=0.45):
    n = int(dur * SR)
    t = np.arange(n) / SR
    atk = np.minimum(1.0, t / 0.006)
    env = atk * np.exp(-t * 3.6 / max(0.08, dur))
    wave_ = (np.sin(2 * math.pi * freq * t)
             + bright * 0.55 * np.sin(2 * math.pi * freq * 2.01 * t + 0.4)
             + bright * 0.25 * np.sin(2 * math.pi * freq * 3.99 * t + 0.9))
    return wave_ * env * amp


def bass_note(freq, dur, amp=0.23):
    n = int(dur * SR)
    t = np.arange(n) / SR
    env = env_adsr(n, 0.03, 0.35, 0.65, 0.35)
    w = np.sin(2 * math.pi * freq * t) + 0.18 * np.sin(2 * math.pi * freq * 2 * t)
    return w * env * amp


def kick(dur=0.42, amp=0.30):
    n = int(dur * SR)
    t = np.arange(n) / SR
    freq = 92.0 * np.exp(-t * 18) + 42.0
    phase = 2 * math.pi * np.cumsum(freq) / SR
    env = np.exp(-t * 8.5) * np.minimum(1.0, t / 0.003)
    click = np.exp(-t * 90.0) * 0.18
    return (np.sin(phase) + click) * env * amp


def shaker(dur=0.16, amp=0.035, seed=0):
    rng = np.random.default_rng(seed)
    n = int(dur * SR)
    t = np.arange(n) / SR
    noise = rng.normal(0, 1, n)
    noise = np.diff(np.concatenate([[0.0], noise]))
    env = np.exp(-t * 35.0)
    return noise * env * amp


def make_track(bpm, bars, chords, melody, out_name, percussion=True, seed=7):
    beat = 60.0 / bpm
    bar_dur = beat * 4
    total = bar_dur * bars + 2.4
    n = int(total * SR)
    left = np.zeros(n)
    right = np.zeros(n)
    rng = np.random.default_rng(seed)

    def add(sig, start, pan=0.0, gain_l=1.0, gain_r=1.0):
        i = int(start * SR)
        if i >= n:
            return
        m = min(len(sig), n - i)
        gl = math.sqrt((1.0 - pan) / 2.0) * gain_l
        gr = math.sqrt((1.0 + pan) / 2.0) * gain_r
        left[i:i + m] += sig[:m] * gl
        right[i:i + m] += sig[:m] * gr

    def add_stereo(sig_l, sig_r, start, gain=1.0):
        i = int(start * SR)
        if i >= n:
            return
        m = min(len(sig_l), n - i)
        left[i:i + m] += sig_l[:m] * gain
        right[i:i + m] += sig_r[:m] * gain

    for bar in range(bars):
        chord = chords[bar % len(chords)]
        t0 = bar * bar_dur
        pad_notes = chord["pad"]
        for j, note in enumerate(pad_notes):
            l, r = pad_note(midi_hz(note), bar_dur + 0.8, amp=0.075 / max(1, len(pad_notes) ** 0.45),
                            detune=0.0012 + j * 0.0002)
            add_stereo(l, r, t0)
        # 低音
        add(bass_note(midi_hz(chord["bass"]), bar_dur * 0.92, amp=0.18), t0 + 0.02, pan=0.0)

        # 分解和弦：每半拍一个音
        arp = chord["arp"]
        steps = 8
        for k in range(steps):
            note = arp[(k + (bar % 2)) % len(arp)] + 12 * (k // 4)
            start = t0 + k * (bar_dur / steps)
            amp = 0.12 if k % 4 else 0.15
            amp *= 0.9 + 0.15 * rng.random()
            add(pluck(midi_hz(note), 1.5, amp=amp, bright=0.38),
                start, pan=(-0.5 if k % 2 == 0 else 0.5))

        # 稀疏旋律
        if melody and bar % 2 == 0:
            note = melody[(bar // 2) % len(melody)]
            add(pluck(midi_hz(note), 2.3, amp=0.14, bright=0.65), t0 + beat * 1.5, pan=0.12)
            add(pluck(midi_hz(note + 12), 1.2, amp=0.045, bright=0.8), t0 + beat * 1.5 + 0.02, pan=-0.25)

        if percussion:
            for b in range(4):
                if b in (0, 2):
                    add(kick(), t0 + b * beat, pan=0.0)
                add(shaker(seed=bar * 10 + b), t0 + b * beat + beat * 0.5, pan=0.3)

    # 简易多抽头延迟，营造空间感
    for delay_s, gain, cross in [(0.31, 0.20, 0.30), (0.47, 0.13, 0.25), (0.73, 0.08, 0.4)]:
        d = int(delay_s * SR)
        dl = np.zeros(n)
        dr = np.zeros(n)
        dl[d:] = left[:-d]
        dr[d:] = right[:-d]
        left += (1 - cross) * dl * gain + cross * dr * gain
        right += (1 - cross) * dr * gain + cross * dl * gain

    # 轻微高通，去除低频浑浊
    left = np.append(left[0], left[1:] - 0.985 * left[:-1])
    right = np.append(right[0], right[1:] - 0.985 * right[:-1])

    fade = int(2.0 * SR)
    fade_in = np.minimum(1.0, np.arange(n) / fade)
    fade_out = np.minimum(1.0, np.arange(n)[::-1] / (fade * 1.3))
    left *= fade_in * fade_out
    right *= fade_in * fade_out

    peak = max(np.max(np.abs(left)), np.max(np.abs(right)), 1e-9)
    scale = 0.88 / peak
    left = np.clip(left * scale, -1.0, 1.0)
    right = np.clip(right * scale, -1.0, 1.0)
    stereo = np.empty(n * 2, dtype=np.int16)
    stereo[0::2] = (left * 32767).astype(np.int16)
    stereo[1::2] = (right * 32767).astype(np.int16)

    tmp = tempfile.NamedTemporaryFile(suffix=".wav", delete=False)
    tmp.close()
    with wave.open(tmp.name, "wb") as wf:
        wf.setnchannels(2)
        wf.setsampwidth(2)
        wf.setframerate(SR)
        wf.writeframes(stereo.tobytes())
    os.makedirs(OUT_DIR, exist_ok=True)
    out_path = os.path.join(OUT_DIR, out_name)
    cmd = [FFMPEG, "-y", "-loglevel", "error", "-i", tmp.name,
           "-codec:a", "mp3_mf", "-b:a", "192k", "-ar", "44100", out_path]
    subprocess.run(cmd, check=True)
    # MediaFoundation 编码器写入的时长头不准确，重封装一次补上 Xing 头。
    remux = out_path + ".remux.mp3"
    subprocess.run([FFMPEG, "-y", "-loglevel", "error", "-i", out_path,
                    "-c:a", "copy", "-write_xing", "1", remux], check=True)
    os.replace(remux, out_path)
    os.unlink(tmp.name)
    print("created", out_path, os.path.getsize(out_path))


def main():
    c_maj7 = {"pad": [48, 55, 59, 64, 67], "bass": 36, "arp": [60, 64, 67, 71, 67, 64, 59, 55]}
    a_min7 = {"pad": [45, 52, 55, 60, 64], "bass": 33, "arp": [57, 60, 64, 67, 64, 60, 55, 52]}
    f_maj7 = {"pad": [41, 48, 57, 60, 64], "bass": 29, "arp": [53, 57, 60, 64, 60, 57, 52, 48]}
    g_6 = {"pad": [43, 50, 55, 59, 64], "bass": 31, "arp": [55, 59, 62, 64, 62, 59, 55, 50]}
    make_track(
        bpm=92, bars=16,
        chords=[c_maj7, a_min7, f_maj7, g_6],
        melody=[72, 76, 79, 74, 72, 79, 76, 71],
        out_name="晨光.mp3", percussion=True, seed=11,
    )

    a_min9 = {"pad": [45, 52, 55, 59, 60, 64], "bass": 33, "arp": [57, 60, 64, 67, 71, 67, 64, 60]}
    f_maj9 = {"pad": [41, 48, 52, 55, 60, 64], "bass": 29, "arp": [53, 57, 60, 64, 67, 64, 60, 57]}
    c_maj7 = {"pad": [48, 55, 59, 64, 67], "bass": 36, "arp": [55, 59, 64, 67, 71, 67, 64, 59]}
    g_6 = {"pad": [43, 50, 55, 59, 62, 64], "bass": 31, "arp": [55, 59, 62, 67, 71, 67, 62, 59]}
    make_track(
        bpm=70, bars=14,
        chords=[a_min9, f_maj9, c_maj7, g_6],
        melody=[69, 72, 76, 79, 76, 72, 74, 71],
        out_name="湖畔.mp3", percussion=False, seed=23,
    )


if __name__ == "__main__":
    main()

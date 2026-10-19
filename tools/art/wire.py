"""Rewrites a monster's tinted sprite class so it loads its own sheet through ModMobFrames.

wire(java_file, class_name, base_class, texture, w, h, extra_body='', idle_fps=3, run_fps=12)
Everything inside the old class body (tints, scales) is replaced."""
import os
import re

JAVA = os.path.join(os.path.dirname(__file__), '..', '..', 'core', 'src', 'main', 'java',
                    'com', 'shatteredpixel', 'shatteredpixeldungeon')


def find_class(src, name):
	m = re.search(r'(\t*)public static class ' + name + r' extends ([\w.]+)\s*\{', src)
	if not m:
		raise ValueError(name)
	depth, i = 0, m.end() - 1
	while True:
		if src[i] == '{':
			depth += 1
		elif src[i] == '}':
			depth -= 1
			if depth == 0:
				return m.start(), i + 1, m.group(1), m.group(2)
		i += 1


def wire(path, name, texture, w, h, extra='', idle_fps=3, run_fps=12, base=None):
	full = os.path.join(JAVA, path)
	src = open(full).read()
	a, b, ind, old_base = find_class(src, name)
	base = base or old_base
	t = ind + '\t'
	body = (f"{ind}public static class {name} extends {base} {{\n"
	        f"{t}public {name}(){{\n"
	        f"{t}\tsuper();\n"
	        f"{t}\tModMobFrames.apply( this, \"{texture}\", {w}, {h}, {idle_fps}, {run_fps} );\n"
	        f"{t}}}\n")
	if extra:
		body += '\n'.join(t + line if line else line for line in extra.strip('\n').split('\n')) + '\n'
	body += f"{ind}}}"
	src = src[:a] + body + src[b:]
	imp = 'import com.shatteredpixel.shatteredpixeldungeon.sprites.ModMobFrames;'
	if imp not in src:
		src = re.sub(r'(package [\w.]+;\n)', r'\1\n' + imp + '\n', src, count=1)
	open(full, 'w').write(src)

import sys, importlib
from PIL import Image
import pix
mod, cls, tiers, frames, out = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5]
m = importlib.import_module(mod)
h = getattr(m, cls)()
imgs = []
for t in [int(x) for x in tiers.split(',')]:
	row = h.row(t)
	for f in [int(x) for x in frames.split(',')]:
		imgs.append(row[f].image())
pix.preview(imgs, out, scale=int(sys.argv[6]) if len(sys.argv) > 6 else 14)

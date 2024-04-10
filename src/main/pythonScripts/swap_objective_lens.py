import sys

from smartpath import *
from smartpath_qpscope import *

sp = smartpath(core)
qp = smartpath_qpscope()

## checks what is current

if core.get_property(*camm.obj_slider) == camm_.CAMM_20X_BF.objective_position_label:
    camm.imaging = camm_.CAMM_20X_BF
if core.get_property(*camm.obj_slider) == camm_.CAMM_4X_BF.objective_position_label:
    camm.imaging = camm_.CAMM_4X_BF
## TODO: implement CAMM_20x_MPM/BF update from Core Camera
if core.get_property(*camm.obj_slider) == camm_.CAMM_20X_MPM.objective_position_label:
    camm.imaging = camm_.CAMM_20X_BF


if len(sys.argv) == 2:
    if sys.argv[1].upper() == "20X":
        sp.swap_objective_lens(core, camm, camm_.CAMM_20X_BF)
        print("QP: moved to 20X")
    elif sys.argv[1].upper() == "4X":
        sp.swap_objective_lens(core, camm, camm_.CAMM_4X_BF)
        print("QP: moved to 4X")
    else:
        print(f"{sys.argv}", file=sys.stderr)
        sys.exit(1)
else:
    print("Expected two arguments, X and Y as doubles", file=sys.stderr)
    sys.exit(1)

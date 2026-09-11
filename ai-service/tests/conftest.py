import sys
from pathlib import Path

# 让 pytest 能 import 项目根目录的 app 包
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

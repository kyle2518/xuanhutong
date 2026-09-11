"""十八反十九畏 确定性规则引擎。

数据驱动，可单测、可审计 —— 与 LLM 混合，构成「模型提议 + 规则校验」的安全护栏。
"""
import itertools
import re

# 乌头类：川乌/草乌/附子/乌头 在配伍禁忌中视为同一类
_WUTOU_GROUP = {"乌头", "川乌", "草乌", "附子"}

# 十八反（相反）
SHIBA_FAN: list[tuple[set[str], set[str]]] = [
    ({"甘草"}, {"甘遂", "京大戟", "大戟", "海藻", "芫花"}),
    (_WUTOU_GROUP, {"贝母", "川贝母", "浙贝母", "瓜蒌", "半夏", "白蔹", "白及"}),
    ({"藜芦"}, {"人参", "沙参", "丹参", "玄参", "细辛", "芍药", "白芍", "赤芍"}),
]

# 十九畏（相畏）
SHIJIU_WEI: list[tuple[set[str], set[str]]] = [
    ({"硫黄"}, {"朴硝"}),
    ({"巴豆"}, {"牵牛", "牵牛子"}),
    ({"丁香"}, {"郁金"}),
    (_WUTOU_GROUP, {"犀角", "水牛角"}),
    ({"牙硝"}, {"三棱"}),
    ({"官桂", "肉桂", "桂"}, {"石脂", "赤石脂"}),
    ({"人参"}, {"五灵脂"}),
]


_BRACKET_CONTENT = re.compile(r"[（(][^）)]*[）)]")


def normalize(name: str) -> str:
    """去掉括号及其内容（炮制方法等）、空白，做基础归一。"""
    return _BRACKET_CONTENT.sub("", name).strip()


def check_conflicts(herbs: list[str]) -> list[str]:
    """返回所有冲突对的描述列表；空列表表示无配伍禁忌。"""
    herbs = [normalize(h) for h in herbs if h and h.strip()]
    conflicts: list[str] = []
    for a, b in itertools.combinations(herbs, 2):
        for relation, table in (("十八反", SHIBA_FAN), ("十九畏", SHIJIU_WEI)):
            for left, right in table:
                if (a in left and b in right) or (b in left and a in right):
                    conflicts.append(f"{a} 与 {b} 属{relation}配伍禁忌")
                    break
    return conflicts

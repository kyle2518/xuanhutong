from app.rules.shibafan_shijiuwei import check_conflicts


def test_shiba_fan_gancao_ganzui():
    conflicts = check_conflicts(["甘草", "甘遂"])
    assert conflicts, "甘草反甘遂应命中"


def test_wutou_group_normalized():
    # 附子与半夏冲突（乌头类十八反）
    conflicts = check_conflicts(["附子", "半夏"])
    assert conflicts


def test_shijiu_wei_renshen_wulingzhi():
    conflicts = check_conflicts(["人参", "五灵脂"])
    assert conflicts


def test_no_conflict():
    conflicts = check_conflicts(["桂枝", "芍药", "生姜", "大枣"])
    assert conflicts == []


def test_normalize_brackets():
    # 带括号归一
    conflicts = check_conflicts(["甘草（炙）", "甘遂"])
    assert conflicts

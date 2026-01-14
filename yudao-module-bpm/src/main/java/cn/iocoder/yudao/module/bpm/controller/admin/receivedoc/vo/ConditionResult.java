package cn.iocoder.yudao.module.bpm.controller.admin.receivedoc.vo;

public  class ConditionResult {
    public String key;
    public String value;

    public ConditionResult(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Key: " + key + ", Value: " + value;
    }
}
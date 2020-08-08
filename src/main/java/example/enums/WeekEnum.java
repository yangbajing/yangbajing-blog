package example.enums;

/**
 * @author yangjing (yang.xunjing@qq.com)
 * @date 2020-08-08 13:03
 */
public enum WeekEnum {
    MONDAY(1, "周一"),
    TUESDAY(2, "周二"),
    WEDNESDAY(3, "周三"),
    THURSDAY(4, "周四"),
    FRIDAY(5, "周五"),
    SATURDAY(6, "周六"),
    SUNDAY(7, "周日");

    WeekEnum(int value, String label) {
        this.value = value;
        this.label = label;
    }

    private final int value;
    private final String label;

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }
}

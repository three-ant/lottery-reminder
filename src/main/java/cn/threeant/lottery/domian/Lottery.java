package cn.threeant.lottery.domian;

public class Lottery {

    private final String code;
    private final String red;
    private final String blue;

    public Lottery(String code, String red, String blue) {
        this.code = code;
        this.red = red;
        this.blue = blue;
    }

    public String getCode() {
        return code;
    }

    public String getRed() {
        return red;
    }

    public String getBlue() {
        return blue;
    }

}

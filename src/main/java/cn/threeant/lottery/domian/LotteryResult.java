package cn.threeant.lottery.domian;

public class LotteryResult {

    private final String nickName;
    private final String email;
    private final String red;
    private final String blue;
    private final String winningInfo;

    public LotteryResult(String nickName, String email, String red, String blue, String winningInfo) {
        this.nickName = nickName;
        this.email = email;
        this.red = red;
        this.blue = blue;
        this.winningInfo = winningInfo;
    }

    public String getNickName() {
        return nickName;
    }

    public String getEmail() {
        return email;
    }

    public String getRed() {
        return red;
    }

    public String getBlue() {
        return blue;
    }

    public String getWinningInfo() {
        return winningInfo;
    }
}

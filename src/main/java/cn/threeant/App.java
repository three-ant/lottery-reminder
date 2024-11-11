package cn.threeant;

import cn.threeant.lottery.LotteryReminder;
import cn.threeant.lottery.domian.Lottery;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        Lottery latestResult = LotteryReminder.getLatestLotteryResult();
        if (latestResult != null) {
            LotteryReminder.compareResults(latestResult);
        }
    }
}

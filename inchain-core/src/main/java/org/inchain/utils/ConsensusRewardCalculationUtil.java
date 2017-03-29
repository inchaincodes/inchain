package org.inchain.utils;

import org.inchain.Configure;
import org.inchain.core.Coin;

/**
 * 共识奖励计算工具
 * @author ln
 *
 */
public final class ConsensusRewardCalculationUtil {

	/**
	 * 奖励总数，总理的十分之一
	 */
	public final static Coin TOTAL_REWARD = Coin.MAX.div(10);
	/**
	 * 初始奖励系数，也就是第一个发放奖励的区块开始，奖励多少
	 */
	public final static Coin START_REWARD = Coin.COIN;
	/**
	 * 最小奖励系数，从第一年开始，每年减半，至少达到最小奖励，后面不变
	 */
	public final static Coin MIN_REWARD = START_REWARD.div(10);
	/**
	 * 开始发放奖励的区块高度
	 */
	public final static long START_HEIGHT = 100l;
	/**
	 * 每多少个区块奖励减半，也就是奖励周期，这里设置1年的出块数
	 */
	public final static long REWARD_CYCLE = 3153600l;
	
	/**
	 * 计算共识奖励
	 * 根据传入的区块高度，计算当前的奖励系数
	 * 
	 * @param height
	 * @return Coin
	 */
	public final static Coin calculatReward(long height) {
		if(height < START_HEIGHT) {
			return Coin.ZERO;
		}
		//奖励周期
		long realHeight = (height - START_HEIGHT);
		long coefficient = realHeight / REWARD_CYCLE;
		//奖励系数
		Coin coefficientReward = START_REWARD;
		Coin issued = Coin.ZERO;
		while(coefficient-- > 0) {
			issued = issued.add(coefficientReward.multiply(REWARD_CYCLE));
			coefficientReward = coefficientReward.div(2);
			if(coefficientReward.isLessThan(MIN_REWARD)) {
				coefficientReward = MIN_REWARD;
			}
		}
		
		issued = issued.add(coefficientReward.multiply(realHeight % REWARD_CYCLE));
		
		//余量
		Coin balance = TOTAL_REWARD.subtract(issued);
		if(balance.isLessThan(Coin.ZERO)) {
			coefficientReward = Coin.ZERO;
		} else if(balance.isGreaterThan(Coin.ZERO) && balance.isLessThan(coefficientReward)) {
			coefficientReward = balance;
		}
		
		return coefficientReward;
	}
	
	/**
	 * 计算共识奖励
	 * 根据传入的区块高度，计算当前已产出的奖励总量
	 * 
	 * @param height
	 * @return Coin
	 */
	public final static Coin calculatTotal(long height) {
		if(height < START_HEIGHT) {
			return Coin.ZERO;
		}
		//奖励周期
		long realHeight = (height - START_HEIGHT);
		long coefficient = realHeight / REWARD_CYCLE;
		//奖励系数
		Coin coefficientReward = START_REWARD;
		Coin issued = Coin.ZERO;
		while(coefficient-- > 0) {
			issued = issued.add(coefficientReward.multiply(REWARD_CYCLE));
			coefficientReward = coefficientReward.div(2);
			if(coefficientReward.isLessThan(MIN_REWARD)) {
				coefficientReward = MIN_REWARD;
			}
		}
		return issued.add(coefficientReward.multiply(realHeight % REWARD_CYCLE));
	}

	/**
	 * 计算当前共识所需保证金
	 * @param currentConsensusSize
	 * @return Coin
	 */
	public static Coin calculatRecognizance(int currentConsensusSize) {
		
		//max is (Math.log((double)300)/Math.log((double)2))
		double max = 2468d;
		
		double lgN = Math.log((double)currentConsensusSize)/Math.log((double)2);
		double nlgn = lgN*currentConsensusSize;
		long res = (long) (Configure.CONSENSUS_MAX_RECOGNIZANCE.value * (nlgn/max));
		
		if(res > Configure.CONSENSUS_MAX_RECOGNIZANCE.value) {
			return Configure.CONSENSUS_MAX_RECOGNIZANCE;
		} else if(res < Configure.CONSENSUS_MIN_RECOGNIZANCE.value) {
			return Configure.CONSENSUS_MIN_RECOGNIZANCE;
		} else {
			if(res > Coin.COIN_VALUE * 100000) {
				return Coin.COIN.multiply(res/(Coin.COIN_VALUE * 10000)).multiply(10000);
			} else {
				return Coin.COIN.multiply(res/(Coin.COIN_VALUE * 1000)).multiply(1000);
			}
		}
	}
	
	public static void main(String[] args) {
//		System.out.println(calculatTotal(REWARD_CYCLE * 16 + 3026900));
		
		int size = 308;
		for (int i = 1; i <= size; i ++) {
			System.out.println(i+"   "+calculatRecognizance(i));
		}
	}
}

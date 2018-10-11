package com.investdigital.exchange;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;

/**
 * Created by song.
 */
@RunWith(SpringRunner.class)
public class XETHTest {

/*
    关于存储:
    全局信息:当前最新的交易编号,即交易总量txSize,静态收益出局数量leftSize
    每个节点记录进入时候的交易编号enterSeq,以及进入A区间时刻交易编号enterSeq

*/

//    总收益计算:如果当前节点已经出局则记录静态收益为3,否则计算初始阶段和A\B\C三段的收益之和. 所以后面计算的前提是该几点还没有出局.
    BigDecimal countProfit(int enterSeq, int txSize, int leftSize, BigDecimal[] assistArray, int enterASeq){
        if(enterSeq <= leftSize )
            return new BigDecimal(3);
        BigDecimal init = countInitProfit(enterSeq, txSize, assistArray);
        BigDecimal a = countAProfit(txSize, enterASeq);
        BigDecimal b = countBProfit(enterSeq, txSize, leftSize, assistArray, enterASeq);
        BigDecimal c = countCProfit(enterSeq, txSize);
        return init.add(a).add(b).add(c);
    }

    // 初始收益计算:针对编号小于200的节点.
    // 计算节点在A轮收益(该节点一定是还没出局的,否则不会用到此函数), 该交易序号,当前交易总数,离开交易数量和最近一次离开发生的时刻
    BigDecimal countInitProfit(int enterSeq, int txSize, BigDecimal[] assistArray){
        if(enterSeq > 200) {
            return new BigDecimal(0);
        }
        if(txSize <= 201){
            return assistArray[txSize].subtract( assistArray[enterSeq]);
        }
        return assistArray[200].subtract(assistArray[enterSeq]);

    }
    // 计算节点在A区间的收益,如果不在A区间返回0,否则根据其进入A区间的seq和总量计算
    BigDecimal countAProfit(int txSize, int enterASeq) {
        if(enterASeq == -1){
            return new BigDecimal(0);
        }
        return new BigDecimal(0.001*(txSize - enterASeq));
    }

    // 计算节点在B区间的益,如果不在A区间返回0,否则根据其进入A区间的seq和总量计算
    BigDecimal countBProfit(int enterSeq, int txSize, int leftSize, BigDecimal[] assistArray, int enterASeq){
        // 如果还在C区间 则返回为0
        if(enterSeq + 100 > txSize) {
            return new BigDecimal(0);
        }
        // 只对于编号大于100的交易
        if(enterSeq < 101){
            return new BigDecimal(0);
        }
//        进入B的时刻 = enterSeq + 100;
//        如果在B区间
        if(enterSeq > leftSize + 100){
            // 进入B区间的时刻enterSeq+100
            // 最新进入B区间最后节点txSize-100
            return assistArray[txSize-100].subtract(assistArray[enterSeq+100]);
        }
        // 如果在A区间,
        return assistArray[enterASeq].subtract(assistArray[enterSeq+100]);

    }

    BigDecimal countCProfit(int enterSeq, int txSize) {
        // 前200次进入过程,所有节点不享受C轮
        // 先判断大多情况是 200以上的
        if (enterSeq > 200) {
            // 如果当前轮次与进入轮次比较超过100,则该交易落在B区域,拿到全额100次C区域收益
            if (txSize - enterSeq > 100) {
                return new BigDecimal(0.1);
            } else
                return new BigDecimal(0.001 * (txSize - enterSeq));
        }
        if (enterSeq < 101 || txSize < 200)
            return new BigDecimal(0);
        // enterSeq [101, 200]区间交易
//        enterSeq 101~ 200
//        txSize > 300
//        300 > txSize > enterSeq + 100 == 处于B区间

        if (txSize > 300 || enterSeq < txSize - 100) {
            return new BigDecimal(0.001 * (enterSeq - 100));
        }
        // txSize 200~ 300
        else {
            return new BigDecimal(0.001 * (txSize - 200));
        }
    }


    @Test
    public void testStaticProfit() throws IOException {
        //所有数组让出0 从第一位开始
        int max = 500000;//10000000;//210000001;
        BigDecimal ticketVal = new BigDecimal(1);
        BigDecimal[] assistArray = new BigDecimal[max];
        int[] enterASeq = new int[max];  //记录每个节点进入A区间的时刻
        //初始化进入A区间时刻 -1位不在A区间
        for (int i = 0; i < max; i++) {
            assistArray[i] = new BigDecimal(0);
            enterASeq[i] = -1;
        }
        for (int seq = 2; seq < 201; seq++) { //前200人 每轮均分
            assistArray[seq] = assistArray[seq - 1].add(new BigDecimal(1 * 0.3).divide(new BigDecimal(seq - 1), 9, BigDecimal.ROUND_DOWN));
        }
        //初始化进入A区间时刻, 前101个交易在第200个交易进入时进入A区间
        for (int seq = 1; seq < 101; seq++){
            enterASeq[seq] = 200;
        }
        int leftSize = 0;
        assistArray[201] = new BigDecimal(0);
        for (int seq = 202; seq < max; seq++) { //第201号人进入给前200人
            assistArray[seq] = assistArray[seq - 1].add(new BigDecimal(1 * 0.1).divide(new BigDecimal(seq - 200 - 1), 9, BigDecimal.ROUND_DOWN));
            if(countProfit(leftSize+1, seq, leftSize, assistArray, enterASeq[leftSize+1]).compareTo(new BigDecimal(3)) > 0){
                leftSize ++;
                //假设已有10个出局, i进入时第11号出局,第111号进入A区间, 享受到了i贡献的B区间分红
                enterASeq[leftSize+100] = seq;
                System.out.println(seq);
            }
        }
        int[] count = new int[]{0,0,0,0};
      // 文件输出收益数据
        File file = new File("/Users/song/Downloads/XETHresult.txt");
        if (file.exists()){
            file.delete();
            file.createNewFile();
        }
        FileWriter fw = new FileWriter(file);
        for(int i = 1; i < max; i++){
            BigDecimal profit = countProfit(i, max-1, leftSize, assistArray, enterASeq[i]);
            if(profit.compareTo(new BigDecimal(3))>=0){
                count[3]++;
            }
            else if(profit.compareTo(new BigDecimal(2))>=0){
                count[2]++;
            }
            else if(profit.compareTo(new BigDecimal(1))>=0){
                count[1]++;
            }
            fw.append(profit.toPlainString()+"\n");
        }
        fw.flush();
        fw.close();
        Arrays.stream(count).forEach(System.out::println);
    }
}

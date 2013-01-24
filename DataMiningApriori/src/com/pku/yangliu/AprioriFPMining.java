package com.pku.yangliu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**频繁模式挖掘算法Apriori实现
 * @author yangliu
 * @qq 772330184
 * @blog http://blog.csdn.net/yangliuy
 * @mail yang.liu@pku.edu.cn
 *
 */

public class AprioriFPMining {
	private int minSup;//最小支持度
	private static List<Set<String>> dataTrans;//以List<Set<String>>格式保存的事物数据库,利用Set的有序性
	
	public int getMinSup() {
		return minSup;
	}
	
	public void setMinSup(int minSup) {
		this.minSup = minSup;
	}
	
	/**
	 * @param args
	 */
	 public static void main(String[] args) throws IOException { 
		AprioriFPMining apriori = new AprioriFPMining();
		double [] threshold = {0.25, 0.20, 0.15, 0.10, 0.05};
		String srcFile = "F:/DataMiningSample/FPmining/Mushroom.dat";
		String shortFileName = srcFile.split("/")[3];
        String targetFile = "F:/DataMiningSample/FPmining/" + shortFileName.substring(0, shortFileName.indexOf("."))+"_fp_threshold";
		dataTrans = apriori.readTrans(srcFile);
		for(int k = 0; k < threshold.length; k++){
			System.out.println(srcFile + " threshold: " + threshold[k]);
			long totalItem = 0;
			long totalTime = 0;
			FileWriter tgFileWriter = new FileWriter(targetFile + (threshold[k]*100));
			apriori.setMinSup((int)(dataTrans.size() * threshold[k]));//原始蘑菇的数据0.25只需要67秒跑出结果，比lwl快15倍
			long startTime = System.currentTimeMillis();
			Map<String, Integer> f1Set = apriori.findFP1Items(dataTrans);
			long endTime = System.currentTimeMillis();
			totalTime += endTime - startTime;
			//频繁1项集信息得加入支持度
			Map<Set<String>, Integer> f1Map = new HashMap<Set<String>, Integer>();
			for(Map.Entry<String, Integer> f1Item : f1Set.entrySet()){
				Set<String> fs = new HashSet<String>();
				fs.add(f1Item.getKey());
				f1Map.put(fs, f1Item.getValue());
			}
			
			totalItem += apriori.printMap(f1Map, tgFileWriter);
			Map<Set<String>, Integer> result = f1Map;
			do {	
				startTime = System.currentTimeMillis();
				result = apriori.genNextKItem(result);
				endTime = System.currentTimeMillis();
				totalTime += endTime - startTime;
				totalItem += apriori.printMap(result, tgFileWriter);
			} while(result.size() != 0);
			tgFileWriter.close();
			System.out.println("共用时：" + totalTime + "ms");
			System.out.println("共有" + totalItem + "项频繁模式");
		}
	}

	 /**由频繁K-1项集生成频繁K项集
	 * @param preMap 保存频繁K项集的map
	 * @param tgFileWriter 输出文件句柄
	 * @return int 频繁i项集的数目
	 * @throws IOException 
	 */
	private Map<Set<String>, Integer> genNextKItem(Map<Set<String>, Integer> preMap) {
		// TODO Auto-generated method stub
		Map<Set<String>, Integer> result = new HashMap<Set<String>, Integer>();
		//遍历两个k-1项集生成k项集
		List<Set<String>> preSetArray = new ArrayList<Set<String>>();
		for(Map.Entry<Set<String>, Integer> preMapItem : preMap.entrySet()){
			preSetArray.add(preMapItem.getKey());
		}
		int preSetLength = preSetArray.size();
		for (int i = 0; i < preSetLength - 1; i++) {
			for (int j = i + 1; j < preSetLength; j++) {
				String[] strA1 = preSetArray.get(i).toArray(new String[0]);
				String[] strA2 = preSetArray.get(j).toArray(new String[0]);
				if (isCanLink(strA1, strA2)) { // 判断两个k-1项集是否符合连接成k项集的条件　
					Set<String> set = new TreeSet<String>();
					for (String str : strA1) {
						set.add(str);
					}
					set.add((String) strA2[strA2.length - 1]); // 连接成k项集
					// 判断k项集是否需要剪切掉，如果不需要被cut掉，则加入到k项集列表中
					if (!isNeedCut(preMap, set)) {//由于单调性，必须保证k项集的所有k-1项子集都在preMap中出现，否则就该剪切该k项集
						result.put(set, 0);
					}
				}
			}
		}
		return assertFP(result);//遍历事物数据库，求支持度，确保为频繁项集
	}
	
	/**检测k项集是否该剪切。由于单调性，必须保证k项集的所有k-1项子集都在preMap中出现，否则就该剪切该k项集
	 * @param preMap k-1项频繁集map
	 * @param set 待检测的k项集
	 * @return boolean 是否该剪切
	 * @throws IOException 
	 */
	private boolean isNeedCut(Map<Set<String>, Integer> preMap, Set<String> set) {
		// TODO Auto-generated method stub
		boolean flag = false;
		List<Set<String>> subSets = getSubSets(set);
		for(Set<String> subSet : subSets){
			if(!preMap.containsKey(subSet)){
				flag = true;
				break;
			}
		}
		return flag;
	}

	/**获取k项集set的所有k-1项子集
	 * @param set 频繁k项集
	 * @return List<Set<String>> 所有k-1项子集容器
	 * @throws IOException 
	 */
	private List<Set<String>> getSubSets(Set<String> set) {
		// TODO Auto-generated method stub
		String[] setArray = set.toArray(new String[0]);
		List<Set<String>> result = new ArrayList<Set<String>>();
		for(int i = 0; i < setArray.length; i++){
			Set<String> subSet = new HashSet<String>();
			for(int j = 0; j < setArray.length; j++){
				if(j != i) subSet.add(setArray[j]);
			}
			result.add(subSet);
		}
		return result;
	}

	/**遍历事物数据库，求支持度，确保为频繁项集
	 * @param allKItem 候选频繁k项集
	 * @return Map<Set<String>, Integer> 支持度大于阈值的频繁项集和支持度map
	 * @throws IOException 
	 */
	private Map<Set<String>, Integer> assertFP(
			Map<Set<String>, Integer> allKItem) {
		// TODO Auto-generated method stub
		Map<Set<String>, Integer> result = new HashMap<Set<String>, Integer>();
		for(Set<String> kItem : allKItem.keySet()){
			for(Set<String> data : dataTrans){
				boolean flag = true;
				for(String str : kItem){
					if(!data.contains(str)){
						flag = false;
						break;
					}
				}
				if(flag) allKItem.put(kItem, allKItem.get(kItem) + 1);
			}
			if(allKItem.get(kItem) >= minSup) {
				result.put(kItem, allKItem.get(kItem));
			}
		}
		return result;
	}

	/**检测两个频繁K项集是否可以连接，连接条件是只有最后一个项不同
	 * @param strA1 k项集1
	 * @param strA1 k项集2
	 * @return boolean 是否可以连接
	 * @throws IOException 
	 */
	private boolean isCanLink(String[] strA1, String[] strA2) {
		// TODO Auto-generated method stub
		boolean flag = true;
		if(strA1.length != strA2.length){
			return false;
		}else {
			for(int i = 0; i < strA1.length - 1; i++){
				if(!strA1[i].equals(strA2[i])){
					flag = false;
					break;
				}
			}
			if(strA1[strA1.length -1].equals(strA2[strA1.length -1])){
				flag = false;
			}
		}
		return flag;
	}

	/**将频繁i项集的内容及支持度输出到文件 格式为 模式:支持度
	 * @param f1Map 保存频繁i项集的容器<i项集 , 支持度>
	 * @param tgFileWriter 输出文件句柄
	 * @return int 频繁i项集的数目
	 * @throws IOException 
	 */
	private int printMap(Map<Set<String>, Integer> f1Map, FileWriter tgFileWriter) throws IOException {
		// TODO Auto-generated method stub
		for(Map.Entry<Set<String>, Integer> f1MapItem : f1Map.entrySet()){
			for(String p : f1MapItem.getKey()){
				tgFileWriter.append(p + " ");
			}
			tgFileWriter.append(": " + f1MapItem.getValue() + "\n");
		}
		tgFileWriter.flush();
		return f1Map.size();
	}
	
	/**生成频繁1项集
	 * @param fileDir 事务文件目录
	 * @return Map<String, Integer> 保存频繁1项集的容器<1项集 , 支持度>
	 * @throws IOException 
	 */
	private Map<String, Integer> findFP1Items(List<Set<String>> dataTrans) {
		// TODO Auto-generated method stub
		Map<String, Integer> result = new HashMap<String, Integer>();
		Map<String, Integer> itemCount = new HashMap<String, Integer>();
		for(Set<String> ds : dataTrans){
			for(String d : ds){
				if(itemCount.containsKey(d)){
					itemCount.put(d, itemCount.get(d) + 1);
				} else {
					itemCount.put(d, 1);
				}
			}
		}
		
		for(Map.Entry<String, Integer> ic : itemCount.entrySet()){
			if(ic.getValue() >= minSup){
				result.put(ic.getKey(), ic.getValue());
			}
		}
		return result;
	}

	/**读取事务数据库
	 * @param fileDir 事务文件目录
	 * @return List<String> 保存事务的容器
	 * @throws IOException 
	 */
	private List<Set<String>> readTrans(String fileDir) {
		// TODO Auto-generated method stub
		List<Set<String>> records = new ArrayList<Set<String>>(); 
        try { 
            FileReader fr = new FileReader(new File(fileDir)); 
            BufferedReader br = new BufferedReader(fr); 
       
            String line = null; 
            while ((line = br.readLine()) != null) { 
                if (line.trim() != "") { 
                    Set<String> record = new HashSet<String>(); 
                    String[] items = line.split(" "); 
                    for (String item : items) { 
                        record.add(item); 
                    } 
                    records.add(record); 
                } 
            } 
        } catch (IOException e) { 
            System.out.println("读取事务文件失败。"); 
            System.exit(-2); 
        } 
        return records; 
	}
}
 
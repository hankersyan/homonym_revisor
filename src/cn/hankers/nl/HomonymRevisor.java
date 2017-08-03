package cn.hankers.nl;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.hankcs.algorithm.AhoCorasickDoubleArrayTrie;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

public class HomonymRevisor {

	private final static String TAG = "HomonymRevisor";

	private boolean _bFuzzyEnabled = true;
	private AhoCorasickDoubleArrayTrie<String> _acdat;

	public static void main(String[] args) {

		// 准备行业词典
		TreeMap<String, String> map = new TreeMap<String, String>();
		
		long time0 = System.currentTimeMillis();
		
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader("med.txt"));
			
		    String line = br.readLine();
		    int comma = 0;
		    while (line != null) {
		    	comma = line.indexOf(',');
		    	if(comma > 0 && comma < line.length()) {
		    		map.put(line.substring(0,  comma), line.substring(comma + 1));
		    	}
		        line = br.readLine();
		    }
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
		    try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		long time1 = System.currentTimeMillis();
		log(TAG, map.size() + " rows loaded, elapsed milliseconds=" + (time1 - time0) );
		
//		map.put("abakawei", "阿巴卡韦");
//		map.put("abeiaiershishoushu", "阿贝埃二氏手术");
//		map.put("abendazuo", "阿苯达唑");
//		map.put("aerrezuerxuehongdanbai", "阿尔热祖尔血红蛋白");
//		map.put("xueyangbaohedu", "血氧饱和度");
//		map.put("tangshizonghezheng", "唐氏综合症");

		// 含错句子
		final String origin = ".病人的血样饱和度是64%,上，可能是唐氏中合镇危险了!";

		HomonymRevisor revisor = new HomonymRevisor(map, true);
		
		log(TAG, "HomonymRevisor inited, elapsed milliseconds=" + (System.currentTimeMillis() - time1) );
		
		// 校正结果
		final String revised = revisor.revise(origin);

		log(TAG, origin + "=>" + revised);
	}

	private static void log(String tag, String msg) {
		System.out.print(tag + "  " + msg + "\r\n");
	}

	@SuppressWarnings("unused")
	private HomonymRevisor() {
	}

	public HomonymRevisor(Map<String, String> dict, boolean fuzzyEnabled) {
		_bFuzzyEnabled = fuzzyEnabled;
		
		ArrayList<String> conflicts = new ArrayList<>();
		
		TreeMap<String, String> map = new TreeMap<String, String>();
		for (Map.Entry<String, String> entry : dict.entrySet()) {
			String key = entry.getKey();
			String val = entry.getValue();
			String calcuatedPy = calcFuzzyPinYinForString(val, key);
			if (!map.containsKey(calcuatedPy)) {
				map.put(calcuatedPy, val);
			} else {
				log(TAG, "conflict found:" + calcuatedPy + " for " + val + " => " + map.get(calcuatedPy));
				conflicts.add(calcuatedPy);
			}
		}
		
		if(conflicts.size() > 0) {
			log(TAG, "Both of the conflicts will NOT work!");
			log(TAG, "Please REMOVE the conflict from your library!");
			for(String key : conflicts) {
				map.remove(key);
			}
		}

		// Build an AhoCorasickDoubleArrayTrie
		_acdat = new AhoCorasickDoubleArrayTrie<String>();
		_acdat.build(map);
	}

	public String revise(String chineseStr) {
		ArrayList<String> sentences = splite2Sentence(chineseStr);
		log(TAG, sentences.toString());

		StringBuilder builder = new StringBuilder();

		for (String sentence : sentences) {
			if (sentence.length() > 1) {
				builder.append(correctSentence(sentence));
			} else {
				builder.append(sentence);
			}
		}
		return builder.toString();
	}

	private String getSubstringByPinYinPosition(final String src, int pyStart, int pyEnd, int[] pyLengthList) {
		int cumulus = 0;
		int startIndex = 0;
		int endIndex = 0;
		for (int i = 0; i < pyLengthList.length; ++i) {
			cumulus += pyLengthList[i];
			if (cumulus == pyStart) {
				startIndex = i + 1;
			} else if (cumulus == pyEnd) {
				endIndex = i + 1;
			} else if (cumulus > pyEnd) {
				break;
			}
		}
		return src.substring(startIndex, endIndex);
	}

	private int minEditDistance(String word1, String word2) {
		int len1 = word1.length();
		int len2 = word2.length();

		// len1+1, len2+1, because finally return dp[len1][len2]
		int[][] dp = new int[len1 + 1][len2 + 1];

		for (int i = 0; i <= len1; i++) {
			dp[i][0] = i;
		}

		for (int j = 0; j <= len2; j++) {
			dp[0][j] = j;
		}

		// iterate though, and check last char
		for (int i = 0; i < len1; i++) {
			char c1 = word1.charAt(i);
			for (int j = 0; j < len2; j++) {
				char c2 = word2.charAt(j);

				// if last two chars equal
				if (c1 == c2) {
					// update dp value for +1 length
					dp[i + 1][j + 1] = dp[i][j];
				} else {
					int replace = dp[i][j] + 1;
					int insert = dp[i][j + 1] + 1;
					int delete = dp[i + 1][j] + 1;

					int min = replace > insert ? insert : replace;
					min = delete > min ? min : delete;
					dp[i + 1][j + 1] = min;
				}
			}
		}

		return dp[len1][len2];
	}

	private ArrayList<String> splite2Sentence(String src) {
		ArrayList<String> sentences = new ArrayList<>();
		int start = 0;
		for (int i = 0; i < src.length(); ++i) {
			char c = src.charAt(i);
			String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c);
			if (pinyinArray == null) {
				if (start == i) {
					sentences.add(src.substring(start, i + 1));
				} else {
					sentences.add(src.substring(start, i));
					sentences.add(src.substring(i, i + 1));
				}
				start = i + 1;
			}
		}
		if (start < src.length() - 1) {
			sentences.add(src.substring(start, src.length()));
		}
		return sentences;
	}

	private String correctSentence(String origin) {
		int[] pyLengthList = new int[origin.length()];
		String[] pyList = new String[origin.length()];
		StringBuilder builder = new StringBuilder();
		HanyuPinyinOutputFormat outputFormat = new HanyuPinyinOutputFormat();
		outputFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
		try {
			int idx = 0;
			for (char c : origin.toCharArray()) {
				String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, outputFormat);
				if (pinyinArray != null) {
					String calcutedPy = calcFuzzyPinYinForWord(pinyinArray[0]);
					pyLengthList[idx] = calcutedPy.length();
					pyList[idx] = calcutedPy;
					builder.append(calcutedPy);
				} else {
					pyLengthList[idx] = 1;
					pyList[idx] = String.valueOf(c);
					builder.append(c);
				}
				idx++;
			}
		} catch (BadHanyuPinyinOutputFormatCombination e1) {
			e1.printStackTrace();
		}

		String result = origin;
		final String text = builder.toString();
		log(TAG, text);
		List<AhoCorasickDoubleArrayTrie<String>.Hit<String>> wordList = _acdat.parseText(text);
		for (AhoCorasickDoubleArrayTrie<String>.Hit<String> hit : wordList) {
			String originSub = getSubstringByPinYinPosition(origin, hit.begin, hit.end, pyLengthList);
			log(TAG, String.format("parseText=%d,%d,%s,original=%s", hit.begin, hit.end, hit.value, originSub));
			if (minEditDistance(originSub, hit.value) < 3) {
				result = result.replace(originSub, hit.value);
			}
		}
		log(TAG, "Result=" + result);
		return result;
	}

	private String calcFuzzyPinYinForString(String hanz, String originPinyin) {
		if (!_bFuzzyEnabled)
			return originPinyin;

		HanyuPinyinOutputFormat outputFormat = new HanyuPinyinOutputFormat();
		outputFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);

		ArrayList<String> list = new ArrayList<>();

		int offset = 0;

		for (int i = 0; i < hanz.length(); ++i) {
			char c = hanz.charAt(i);
			try {
				String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, outputFormat);
				if (pinyinArray != null) {
					boolean found = false;
					for (String py : pinyinArray) {
						if (originPinyin.startsWith(py, offset)) {
							list.add(py);
							offset += py.length();
							found = true;
							break;
						}
					}
					if (!found) {
						list.add(pinyinArray[0]);
						offset += pinyinArray[0].length();
					}
				} else {
					list.add(String.valueOf(c));
				}
			} catch (BadHanyuPinyinOutputFormatCombination e1) {
				e1.printStackTrace();
			}
		}

		StringBuilder builder = new StringBuilder();

		for (String itm : list) {
			String temp = calcFuzzyPinYinForWord(itm);
			builder.append(temp);
		}

		return builder.toString();
	}

	private String calcFuzzyPinYinForWord(String pinyin) {
		if (!_bFuzzyEnabled)
			return pinyin;

		String temp = pinyin;

		if (temp.startsWith("sh")) {
			temp = "s" + temp.substring(2);
		} else if (temp.startsWith("ch")) {
			temp = "c" + temp.substring(2);
		} else if (temp.startsWith("zh")) {
			temp = "z" + temp.substring(2);
		} else if (temp.startsWith("n")) {
			temp = "l" + temp.substring(1);
		} else if (temp.startsWith("r")) {
			temp = "l" + temp.substring(1);
		} else if (temp.startsWith("h")) {
			temp = "f" + temp.substring(1);
		}

//		if (temp.endsWith("iang")) {
//			temp = temp.substring(0, temp.length() - 4) + "ian";
//		} else if (temp.endsWith("uang")) {
//			temp = temp.substring(0, temp.length() - 4) + "uan";
//		} else if (temp.endsWith("ang")) {
//			temp = temp.substring(0, temp.length() - 3) + "an";
//		} else if (temp.endsWith("eng")) {
//			temp = temp.substring(0, temp.length() - 3) + "en";
//		} else if (temp.endsWith("ing")) {
//			temp = temp.substring(0, temp.length() - 3) + "in";
//		}
		
		if (temp.endsWith("eng")) {
			temp = temp.substring(0, temp.length() - 3) + "en";
		} else if (temp.endsWith("ing")) {
			temp = temp.substring(0, temp.length() - 3) + "in";
		}

		return temp;
	}
}
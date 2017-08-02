homonym_revisor
==========================================================================
作者: hankersyan  
简介: 基于特定词典的中文近音词校正  

介绍
-----------

在指定词典的环境下，校正中文近音词，支持模糊音。本项目可应用于某些特定行业，提高第三方语音翻译后的准确率。


例子
------------------------------------

		// 准备行业词典
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put("abakawei", "阿巴卡韦");
		map.put("abeiaiershishoushu", "阿贝埃二氏手术");
		map.put("abendazuo", "阿苯达唑");
		map.put("aerrezuerxuehongdanbai", "阿尔热祖尔血红蛋白");
		map.put("xueyangbaohedu", "血氧饱和度");
		map.put("tangshizonghezheng", "唐氏综合症");

		// 含错句子
		final String origin = ".病人的血样饱和度是64%,上，可能是唐氏中合镇危险了!";

		HomonymRevisor revisor = new HomonymRevisor(map, true);
		
		// 校正结果
		final String revised = revisor.revise(origin);

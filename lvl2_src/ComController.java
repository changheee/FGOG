package merp.com.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.View;

import com.cleopatra.i18n.I18N;
import com.cleopatra.protocol.data.DataRequest;
import com.cleopatra.protocol.data.ParameterGroup;
import com.cleopatra.spring.JSONDataView;

import egovframework.rte.fdl.property.EgovPropertyService;
import merp.com.service.ComService;
import merp.framework.log.ComLogService;
import merp.framework.utils.ComClientUtil;
import merp.framework.utils.ComDateUtil;
import merp.framework.utils.ComStringUtil;
import merp.sd.cs.service.SDCS0101Service;

/**
 * ComController
 * @author 김광호
 * @since 2019.03.13
 * @version 1.0
 * @see
 *
 * <pre>
 *  Modification Information
 *  수정일      		수정자        수정내용
 *  ---------- -------- ---------------------------
 *  2019.03.13  김광호        최초 생성
 * </pre>
 *
 * Copyright (C) 2019 by NDS., All rights reserved.
 */

@Controller
public class ComController {

	protected Logger log = LogManager.getLogger(this.getClass());

    @Resource(name = "comService")
    private ComService comService;

	@Resource(name = "propertiesService")
	protected EgovPropertyService propertiesService;
	
    @Resource(name = "comLogService")
    private ComLogService comLogService;
    
    @Resource(name = "sdcs0101Service")
    private SDCS0101Service sdcs0101Service;

    /**
	  * 메세지를 로드한다.
	  *
	  * @param DataRequest
	  * @return JSONDataView
	  * @exception Exception
	*/
	@RequestMapping("/com/selectMessageList.do")
	public void selectMessageList( DataRequest dataRequest,HttpServletResponse response) throws Exception {

		Map<String,Object> map = new HashMap<String,Object>();
		List<Map<String,String>> result = comService.selectMessageList(map);
		
		if(result.size() > 0){
			
			Map<String, String> message = result
	                .stream()
	                .collect(Collectors.toMap(s -> (String) s.get("msg_cd"), s -> (String) s.get("msg_conts")));
			
			I18N i18n = new I18N();
			i18n.set("ko", message);
			
			response.setContentType("application/javascript; charset=UTF-8");
			response.setCharacterEncoding("UTF-8");
			
			// OutputStream과 I18N을 연결합니다.
			ServletOutputStream out = response.getOutputStream();
			i18n.writeScript(out);
			
		}

	}
	
	/**
	  * 화면 접속 로그를 저장한다.
	  *
	  * @param DataRequest
	  * @return JSONDataView
	  * @exception Exception
	*/
	@RequestMapping("/com/saveAccessLog.do")
	public void saveAccessLog(DataRequest dataRequest, HttpServletRequest request) throws Exception {
		
		String user_id  = dataRequest.getParameter("_user_id");
		String prg_id = dataRequest.getParameter("_prg_id");
		Map<String, String> map = new HashMap<String,String>();
        String ch_gbcd = "W";
        
		String userAgent = request.getHeader("User-Agent").toUpperCase();
        if(userAgent.indexOf("MOBILE") > -1) {   
        	ch_gbcd = "M";
        }else{
        	ch_gbcd = "W";
        }
        
		map.put("user_id", user_id);
    	map.put("prg_id", prg_id);
    	map.put("prtc_time", "0");
    	map.put("prg_prcs_gbcd", "M");
    	map.put("class_nm", "");
    	map.put("meth_nm", "");
    	map.put("ip", ComClientUtil.getRemoteIP());
    	map.put("ch_gbcd", ch_gbcd);
    	
		comLogService.insertAccessLog(map);

	}
	
	/**
	 * 메소드명	: selectOrgCode
	 * 설	 명	: 코드 조회
	 * @param request
	 * @param response
	 * @param dataRequest
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/com/selectOrgCode.do")
	public View selectOrgCode(DataRequest dataRequest) throws Exception {
		
		ParameterGroup dmParam = dataRequest.getParameterGroup("dmParam");

		if(dmParam != null){
			Map<String, String> mapParam = new HashMap<String, String>();
			mapParam.putAll(dmParam.getSingleValueMap());
			dataRequest.setResponse("dsResult", comService.selectOrgList(mapParam));
			dataRequest.setResponse("dmParam", dmParam.getSingleValueMap());
		}
		
		return new JSONDataView();
	}
	
	/**
	 * 메소드명	: selectUserCode
	 * 설	 명	: 코드 조회
	 * @param request
	 * @param response
	 * @param dataRequest
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/com/selectUserList.do")
	public View selectUserList(DataRequest dataRequest) throws Exception {
		
		ParameterGroup dmParam = dataRequest.getParameterGroup("dmParam");

		if(dmParam != null){
			Map<String, String> mapParam = new HashMap<String, String>();
			mapParam.putAll(dmParam.getSingleValueMap());
			dataRequest.setResponse("dsResult", comService.selectUserList(mapParam));
		}
		
		return new JSONDataView();
	}
	
	/**
	 * 메소드명	: selectCodeList
	 * 설	 명	: 공통 코드 조회
	 * @param request
	 * @param response
	 * @param dataRequest
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/com/selectCodeList.do")
	public View selectCodeList(DataRequest dataRequest) throws Exception {
		
		ParameterGroup dmParam = dataRequest.getParameterGroup("dmParam");

		if( dmParam!= null){
			Map<String, String> mapParam = new HashMap<String, String>();
			mapParam.putAll(dmParam.getSingleValueMap());
			dataRequest.setResponse("dsResult", comService.selectCommonCode(mapParam));
			dataRequest.setResponse("dmParam", dmParam.getSingleValueMap());
		}

		return new JSONDataView();
	}
	
	/**
	 * 메소드명	: selectCommonCodeList
	 * 설	 명	: 다건의 공통 코드 조회(init 시점에서 콤보박스 구성)
	 * @param request
	 * @param response
	 * @param dataRequest
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/com/selectCommonCodeList.do")
	public View selectCommonCodeList(DataRequest dataRequest) throws Exception {
		
		String grpCdList = dataRequest.getParameter("grpCdList");
		String datasetList = dataRequest.getParameter("datasetList");
		String query_id = dataRequest.getParameter("query_id");
		List<String> arrGrpCd = null;
		List<String> arrDataSet = null;
		
		if(!ComStringUtil.isEmpty(grpCdList)){
			arrGrpCd = Arrays.asList(grpCdList.split(","));
		}
		
		if(!ComStringUtil.isEmpty(datasetList)){
			arrDataSet =  Arrays.asList(datasetList.split(","));
		}
		
		Map<String, String> mapParam = new HashMap<String, String>();
		mapParam.put("query_id", query_id);
		
		if(arrGrpCd != null && arrDataSet != null){
			int length = arrDataSet.size();
			for(int i = 0; i < length; i++){
				mapParam.put("cocd_grp_cd", arrGrpCd.get(i));
				dataRequest.setResponse(arrDataSet.get(i), comService.selectCommonCode(mapParam));
			}
		}

		return new JSONDataView();
	}
	
	/**
	 * 메소드명	: selectCommonCode
	 * 설	 명	: 단일 공통 코드 조회(init 시점에서 콤보박스 구성)
	 * @param request
	 * @param response
	 * @param dataRequest
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/com/selectCommonCode.do")
	public View selectCommonCode(DataRequest dataRequest) throws Exception {
		
		String cocdGrpCd = dataRequest.getParameter("cocd_grp_cd");
		String dataset = dataRequest.getParameter("dataset");
		String query_id = dataRequest.getParameter("query_id");
		
		Map<String, String> mapParam = new HashMap<String, String>();
		
		if(cocdGrpCd != null && !cocdGrpCd.equals("")){
			mapParam.put("query_id", query_id);
			mapParam.put("cocd_grp_cd", cocdGrpCd);
			dataRequest.setResponse(dataset, comService.selectCommonCode(mapParam));
		}

		return new JSONDataView();
	}

	/**
	 * 메소드명	: selectCommonReq
	 * 설	 명	: 공통 조회
	 * @param request
	 * @param response
	 * @param dataRequest
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/com/selectCommonReq.do")
	public View selectCommonReq(DataRequest dataRequest) throws Exception {
		
		String dataSet = dataRequest.getParameter("dataSet");
		String paramSet = dataRequest.getParameter("paramSet");
		String queryId = dataRequest.getParameter("queryId");
		
		ParameterGroup dmParam = dataRequest.getParameterGroup(paramSet);
		
		Map<String, String> mapParam = new HashMap<String, String>();
		mapParam.put("query_id", queryId);
		
		if(dmParam != null){
			mapParam.putAll(dmParam.getSingleValueMap());
		}
		
		dataRequest.setResponse(dataSet, comService.selectCommonReq(mapParam));
		
		return new JSONDataView();
	}
	
	/**
	 * 메소드명	: selectAllItemGbList
	 * 설	 명	: 품목 코드 조회
	 * @param request
	 * @param response
	 * @param dataRequest
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/com/selectAllItemGbList.do")
	public View selectAllItemGbList(DataRequest dataRequest) throws Exception {
		
		ParameterGroup dmParam = dataRequest.getParameterGroup("dmParam");
		Map<String, String> mapParam = new HashMap<String, String>();
		
		mapParam.putAll(dmParam.getSingleValueMap());
		String type = dmParam.getValue("type");
		
		if(type.equals("ITEM")){
			mapParam.put("query_id", "merp.mapper.com.selectItemGbList");
			dataRequest.setResponse("dsCodeItem", comService.selectCommonReq(mapParam));
		} else if( type.equals("LSSPE")) {
			mapParam.put("query_id", "merp.mapper.com.selectLsspeList");
			dataRequest.setResponse("dsCodeLsspe", comService.selectCommonReq(mapParam));
		} else if( type.equals("LLSPART")) {
			mapParam.put("query_id", "merp.mapper.com.selectLlspartList");
			dataRequest.setResponse("dsCodeLlspart", comService.selectCommonReq(mapParam));
		} else if( type.equals("PLSPART")) {
			mapParam.put("query_id", "merp.mapper.com.selectPlspartList");
			dataRequest.setResponse("dsCodePlspart", comService.selectCommonReq(mapParam));
		} else if( type.equals("ALL")) {
			mapParam.put("query_id", "merp.mapper.com.selectItemGbList");
			dataRequest.setResponse("dsCodeItem", comService.selectCommonReq(mapParam));
			mapParam.clear();
			mapParam.put("query_id", "merp.mapper.com.selectItemGbAllList");
			dataRequest.setResponse("dsCodeAllItem", comService.selectCommonReq(mapParam));
		}
		
		dataRequest.setResponse("dmParam", dmParam.getSingleValueMap());
		
		return new JSONDataView();
	}
	
	/**
	 * 메소드명	 : selectCustList
	 * 설  명	 : 영업_거래처 팝업을 조회한다.
	 * @param request
	 * @param response
	 * @param dataRequest
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/com/selectCustUserList.do")
	public View selectSdcs0105List(DataRequest dataRequest) throws Exception {
		ParameterGroup dmParam = dataRequest.getParameterGroup("dmParam");
		Map<String, String> mapParam = new HashMap<String, String>();
    	Map<String, String> mapPageIndex = new HashMap<String, String>();
    	
    	mapParam.putAll(dmParam.getSingleValueMap());
    	
    	if(!ComStringUtil.isNullString(mapParam.get("cust_gbcd"))){
    		mapParam.put("cust_gbcd", mapParam.get("cust_gbcd").replace("||", "|"));
    		
    		if(mapParam.get("cust_gbcd").startsWith("|")){
    			mapParam.put("cust_gbcd", mapParam.get("cust_gbcd").substring(1));
    		}
    		
    		if(mapParam.get("cust_gbcd").endsWith("|")){
    			mapParam.put("cust_gbcd", mapParam.get("cust_gbcd").substring(0, mapParam.get("cust_gbcd").length()-1));
    		}
    	}
		
		String type = dmParam.getValue("search_type");
		
		if(type.equals("CUST")){
			ParameterGroup dmPageIndex = dataRequest.getParameterGroup("dmPageIndex");
        	mapParam.put("query_id", "merp.mapper.com.selectCustCnt");
			
        	if(dmPageIndex != null) {
        		int rowSize = ComStringUtil.string2integer(dmPageIndex.getValue("row_size"));
        		String pageYn = ComStringUtil.nullTostring(dmPageIndex.getValue("page_yn"));
        		int pageIdx = 1;
        		if(pageYn.equals("Y")){
        			pageIdx = ComStringUtil.string2integer(dmPageIndex.getValue("page_idx"));
        		}
        		
        		mapParam.put("row_size", ComStringUtil.integer2string(rowSize));
        		mapParam.put("row_offset", ComStringUtil.integer2string(rowSize * (pageIdx - 1)));
        		
        		mapPageIndex.putAll(dmPageIndex.getSingleValueMap());
        		mapPageIndex.put("tot_cnt", comService.selectCommonReq(mapParam).toString().replace("[", "").replace("]", ""));
            }
			
        	mapParam.put("query_id", "merp.mapper.com.selectCustList");
        	dataRequest.setResponse("dsSdCustMst", comService.selectCommonReq(mapParam));
			dataRequest.setResponse("dmPageIndex", mapPageIndex);
			dataRequest.setResponse("dmParam", dmParam.getSingleValueMap());
			mapParam.clear();
		} else if(type.equals("CUST_PO")){
			ParameterGroup dmPageIndex = dataRequest.getParameterGroup("dmPageIndex");
        	mapParam.put("query_id", "merp.mapper.com.selectCustCntPo");
			
        	if(dmPageIndex != null) {
        		int rowSize = ComStringUtil.string2integer(dmPageIndex.getValue("row_size"));
        		String pageYn = ComStringUtil.nullTostring(dmPageIndex.getValue("page_yn"));
        		int pageIdx = 1;
        		if(pageYn.equals("Y")){
        			pageIdx = ComStringUtil.string2integer(dmPageIndex.getValue("page_idx"));
        		}      		
        		mapParam.put("row_size", ComStringUtil.integer2string(rowSize));
        		mapParam.put("row_offset", ComStringUtil.integer2string(rowSize * (pageIdx - 1)));
        		
        		mapPageIndex.putAll(dmPageIndex.getSingleValueMap());
        		mapPageIndex.put("tot_cnt", comService.selectCommonReq(mapParam).toString().replace("[", "").replace("]", ""));
            }
			
        	mapParam.put("query_id", "merp.mapper.com.selectCustListPo");
        	dataRequest.setResponse("dsSdCustMst", comService.selectCommonReq(mapParam));
			dataRequest.setResponse("dmPageIndex", mapPageIndex);
			dataRequest.setResponse("dmParam", dmParam.getSingleValueMap());
			mapParam.clear();
		} else if(type.equals("SD_USER")){
			mapParam.put("query_id", "merp.mapper.com.selectSdUserList");
			dataRequest.setResponse("dsSdUser", comService.selectCommonReq(mapParam));
			dataRequest.setResponse("dmParam", dmParam.getSingleValueMap());
			mapParam.clear();
		}
		
		return new JSONDataView();
	}
	
	/**
	 * 메소드명	: selectItemList
	 * 설	 명	: 품목코드 조회
	 * @param request
	 * @param response
	 * @param dataRequest                       
	 * @return
	 * @throws Exception                                                  
	 */
	@RequestMapping("/com/selectItemList.do")
	public View seleteItemList(DataRequest dataRequest) throws Exception {
		
		ParameterGroup dmParam = dataRequest.getParameterGroup("dmParam");

		if(dmParam != null){
			Map<String, String> mapParam = new HashMap<String, String>();
			mapParam.putAll(dmParam.getSingleValueMap());
			dataRequest.setResponse("dsResult", comService.seleteItemList(mapParam));
			dataRequest.setResponse("dmParam", dmParam.getSingleValueMap());
		}
		
		return new JSONDataView();
	}
	
	/**
	 * 메소드명	: seleteItemDtlList
	 * 설	 명	: 품목 상세조회
	 * @param request
	 * @param response
	 * @param dataRequest                       
	 * @return
	 * @throws Exception                                                  
	 */
	@RequestMapping("/com/selectItemDtlList.do")
	public View seleteItemDtlList(DataRequest dataRequest) throws Exception {
		Map<String, String> mapPageIndex = new HashMap<String, String>();
		ParameterGroup dmPageIndex = dataRequest.getParameterGroup("dmPageIndex");
		
		ParameterGroup dmParam = dataRequest.getParameterGroup("dmParam");
		String dataset = dataRequest.getParameter("dataset");
		String[] arrayCols = new String[]{"lsspe_cd","llspart_cd","lsprd_grd_cd","plspart_cd","brand_cd", "item_kind_gbcd"};
		
		if(dataset == null || "".equals(dataset)) dataset = "dsResult";
		
		if(dmParam != null){
			Map<String, Object> mapParam = new HashMap<String, Object>();
			mapParam.putAll(dmParam.getSingleValueMap());
			
			for(String col : arrayCols) {
				if(mapParam.get(col) != null) {
					 ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
					 HashMap<String, String> map; 	
					 
					 for(String pair: mapParam.get(col).toString().split(",")) {
						 if(pair.split(":").length == 2) {
							 map = new HashMap<String, String>();
							 map.put(col.replaceAll("_cd", "_cocd_grp_cd"), pair.split(":")[0]);
							 map.put(col, pair.split(":")[1]);
							 
							 list.add(map);
							 mapParam.put(col, list);
						 } else if(ComStringUtil.isNotEmpty(pair)){
							 // item_kind_gbcd를 위한 추가 요청 부분
							 map = new HashMap<String, String>();
							 map.put(col, pair);
							 
							 list.add(map);
							 mapParam.put(col, list);
						 } else {
							 map = new HashMap<String, String>();
							 map.put(col.replaceAll("_cd", "_cocd_grp_cd"), "");
							 map.put(col, "");
							 
							 //list.add(map);
						 }
					 }					 
				}
			}
			
			int totCnt = comService.selectItemDtlListCnt(mapParam);
			
			if(dmPageIndex != null) {
        		int rowSize = ComStringUtil.string2integer(dmPageIndex.getValue("row_size"));
        		String pageYn = ComStringUtil.nullTostring(dmPageIndex.getValue("page_yn"));
                int pageIdx = 1;
                if(pageYn.equals("Y")){
                	pageIdx = ComStringUtil.string2integer(dmPageIndex.getValue("page_idx"));
                }
        		
        		mapParam.put("row_size", ComStringUtil.integer2string(rowSize));
        		mapParam.put("row_offset", ComStringUtil.integer2string(rowSize * (pageIdx - 1)));
        		
        		mapPageIndex.putAll(dmPageIndex.getSingleValueMap());
        		mapPageIndex.put("tot_cnt", ComStringUtil.integer2string(totCnt));        		
            }
			
			dataRequest.setResponse(dataset, comService.selectItemDtlList(mapParam));
			dataRequest.setResponse("dmParam", dmParam.getSingleValueMap());
			dataRequest.setResponse("dmPageIndex", mapPageIndex);
			
		}
		
		return new JSONDataView();
	}

	/**
	 * 메소드명	: reportView
	 * 설	 명	: 레포트 화면 호출
	 * @param request
	 * @param response
	 * @param dataRequest                       
	 * @return
	 * @throws Exception                                                  
	 */
	@RequestMapping("/com/reportView.do")
	public View reportView(DataRequest dataRequest, HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		String strContextUrl = "http://"+request.getServerName();
		
		if(request.getServerPort() != 80){
			strContextUrl += ":" + request.getServerPort();
		}
		
		Map<String, Object> message = new HashMap<String, Object>();
		message.put("PRT_DATE", ComDateUtil.toString(new Date(), "yyyy-MM-dd HH:mm:ss", null));
		message.put("PRT_USER_IP", ComClientUtil.getRemoteIP());
		message.put("APP_CONTEXT_URL", strContextUrl);
		
		List<Map<String,String>> result = comService.selectCompanyInfo();
		
		
		if(result.size() == 1) {
			Map<String,String> map = result.get(0);
			
			map.forEach((k,v) -> {
				message.put(k.toUpperCase(), v);
			});
		}
		
		dataRequest.setMetadata(true, message);
		
		return  new JSONDataView();
	}
	
	/**
	 * 메소드명	: sendSmsMsg
	 * 설	 명	: SMS를 발송한다.
	 * @param request
	 * @param response
	 * @param dataRequest                       
	 * @return
	 * @throws Exception                                                  
	 */
	@RequestMapping("/com/sendSmsMsg.do")
	public View sendSmsMsg(DataRequest dataRequest) throws Exception {
		ParameterGroup dmParam = dataRequest.getParameterGroup("dmParam");
		Map<String, Object> dmResult = new HashMap<String, Object>();
		
		if(dmParam != null){
			Map<String, Object> mapParam = new HashMap<String, Object>();
			mapParam.putAll(dmParam.getSingleValueMap());
			comService.insertSmsMsg(mapParam);
			
			//성공메시지 전송
			dmResult.put("isSuccess", true);						
		} else {
			dmResult.put("isSuccess", false);
		}
		
		dataRequest.setResponse("dmResult", dmResult);
		return new JSONDataView();
	}
	
	/**
	 * 메소드명	: selectCertImage
	 * 설	 명	: 증명서 Image URL 호출
	 * @param request
	 * @param response
	 * @param dataRequest                       
	 * @return
	 * @throws Exception                                                  
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@RequestMapping("/com/selectCertImage.do")
	public View selectCertImage(DataRequest dataRequest, HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		String filePath = propertiesService.getString("cert.img.filepath");
		String imgUrl = propertiesService.getString("cert.img.url");
		ParameterGroup dsCertImg = dataRequest.getParameterGroup("dsCertImg");
		List<Map<String,String>> histNoList = dsCertImg.getAllRowList();
		
		Map<String,Object> map = new HashMap<String,Object>();
		List histNo = new ArrayList();
		
		histNoList.forEach(x->{			
			histNo.add(x.get("hist_no"));
		});
		
		map.put("hist_no", histNo);
		
		List<Map<String,String>> result = comService.selectCertImage(map);
		
		result.forEach(x -> {
			
			String fileUrl = x.get("file_url").replace(filePath, imgUrl);
			String fileNm = x.get("file_nm");
			
			x.put("cert_img_url", fileUrl + fileNm);
		});
		
		dataRequest.setResponse("dsCertImg", result);
		
		return  new JSONDataView();
	}
	
	/**
	 * 메소드명	: selectZoneList
	 * 설	 명	: 창고 리스트 조회
	 * @param request
	 * @param response
	 * @param dataRequest                       
	 * @return
	 * @throws Exception                                                  
	 */
	@RequestMapping("/com/selectZoneList.do")
	public View selectZoneList(DataRequest dataRequest) throws Exception {
		
		ParameterGroup dmParam = dataRequest.getParameterGroup("dmParam");

		if(dmParam != null){
			Map<String, String> mapParam = new HashMap<String, String>();
			mapParam.putAll(dmParam.getSingleValueMap());
			dataRequest.setResponse("dsZoneList", comService.selectZoneList(mapParam));			
		}
		
		return new JSONDataView();
	}
}

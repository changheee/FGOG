package merp.com.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.View;

import com.cleopatra.export.Exporter;
import com.cleopatra.export.ExporterFactory;
import com.cleopatra.export.ExporterFactory.EXPORTTYPE;
import com.cleopatra.export.source.DataSource;
import com.cleopatra.export.source.JSONDataSourceBuilder;
import com.cleopatra.export.target.HttpResponseOutputTarget;
import com.cleopatra.export.target.OutputTarget;
import com.cleopatra.protocol.data.DataRequest;
import com.cleopatra.protocol.data.ParameterGroup;
import com.cleopatra.spring.JSONDataView;

import egovframework.rte.fdl.property.EgovPropertyService;
import merp.com.service.ComFileService;
import merp.framework.exception.ComBizException;
import merp.framework.utils.ComFileUtil;
import merp.framework.utils.ComStringUtil;



@Controller
public class ComFileController {
	protected Logger log = LogManager.getLogger(this.getClass());

    @Resource(name = "comFileService")
    private ComFileService comfileService;
	
	@Resource(name = "propertiesService")
	protected EgovPropertyService propertiesService;

    /**
	  * 업로드된 파일 리스트를 조회한다.
	  *
	  * @param DataRequest
	  * @return View
	  * @exception Exception    
	*/
	@RequestMapping("/com/fileList.do")
	public View fileList(DataRequest dataRequest) throws Exception {
		ParameterGroup paramGrp = dataRequest.getParameterGroup("dmParam");
		
		String prgId    = ComStringUtil.nullTostring(paramGrp.getValue("prg_id"));
		String bidizCd   = ComStringUtil.nullTostring(paramGrp.getValue("bzdiv_cd"));
		String fileGbcd = ComStringUtil.nullTostring(paramGrp.getValue("file_gb_id"));
		
    	Map<String, String> map = new HashMap<String, String>();
    	map.put("prg_id"   , prgId);
    	map.put("bzdiv_cd"  , bidizCd);
    	map.put("file_gb_id", fileGbcd);
		
		List<Map<String, Object>> fileInfo = comfileService.selectComFileList(map);
		dataRequest.setResponse("dsUpload", fileInfo);

		return new JSONDataView();
	}
    
    /**
	  * 파일을 업로드한다.
	  *
	  * @param DataRequest
	  * @return View
	  * @exception Exception    
	*/
	@RequestMapping("/com/upload.do")
	public View upload(DataRequest dataRequest)	throws Exception {
		
		Map<String, String> fileInfo = comfileService.uploadComFile(dataRequest);
		
		dataRequest.setResponse("dmParam", fileInfo);		

		return new JSONDataView();
	}
	
	/**
	  * 파일을 다운로드한다.
	  *
	  * @param map
	  * @return ModelAndView
	  * @exception Exception
	*/
	@RequestMapping("/com/fileDownload.do")
	public View fileDownload(HttpServletRequest request, HttpServletResponse response, DataRequest dataRequest) throws Exception {

	   	ParameterGroup dmParamDown = dataRequest.getParameterGroup("dmParamDown");
		
	   	String fileSn = dmParamDown.getValue("file_sno");
		
	    Map<String, String> map = new HashMap<String, String>();
        map.put("file_sno",fileSn);
        
		List<Map<String, Object>> fileList = comfileService.selectComFile(map);
		
		if(fileList != null && fileList.size() > 0){
			String fileUrl =  ComStringUtil.nullTostring(fileList.get(0).get("file_url"));
			String fileName =  ComStringUtil.nullTostring(fileList.get(0).get("file_nm"));
			String realfileName =  ComStringUtil.nullTostring(fileList.get(0).get("real_file_nm"));
			
			try {
				ComFileUtil.fileDownloadWrapper(fileUrl+File.separator+fileName,realfileName,request, response);
			}  catch (ComBizException e) {
				throw new ComBizException(e.getErrMsg());
			} finally {
				
			}
		
		}
		return new JSONDataView();
	}
	  
	/**
	  * 모든 파일을 다운로드한다.
	  *
	  * @param map
	  * @return ModelAndView
	  * @exception Exception
	*/
	@RequestMapping("/com/fileDownloadAll.do")
	public View fileDownloadAll(HttpServletRequest request, HttpServletResponse response, DataRequest dataRequest) throws Exception {
	   	ParameterGroup dmParam = dataRequest.getParameterGroup("dmParam");
		
	   	String prgId = ComStringUtil.nullTostring(dmParam.getValue("prg_id"));
	   	String bzdivCd = ComStringUtil.nullTostring(dmParam.getValue("bzdiv_cd"));
	   	String fileGbcd = ComStringUtil.nullTostring(dmParam.getValue("file_gb_id"));
	   	
	    Map<String, String> map = new HashMap<String, String>();
        map.put("prg_id", ComStringUtil.nullTostring(prgId));
        map.put("bzdiv_cd", ComStringUtil.nullTostring(bzdivCd));
        map.put("file_gb_id", ComStringUtil.nullTostring(fileGbcd));
        
		List<Map<String, Object>> fileList = comfileService.selectComFile(map);
		if(fileList != null && fileList.size() > 0){
			String fileName = (String)fileList.get(0).get("real_file_nm");
			fileName = fileName.substring(0, fileName.lastIndexOf("."));
			if(fileList.size() == 1){
				fileName += ".zip";
			}else{
				fileName += " 외("+(fileList.size()-1)+"개).zip";
			}
			
			String strDestDir = propertiesService.getString("fileTempPath");
			new File(strDestDir).mkdirs();
			
			FileInputStream fin = null;
			FileOutputStream fout = null;
			ZipOutputStream zos = null;
			ZipEntry zen = null;
			
			try {
				fout = new FileOutputStream(strDestDir + File.separator + fileName);
				zos = new ZipOutputStream(fout);
				
				Map<String, Object> fileItem;
				File file = null;
				int fLength;
				byte [] buf = new byte[2048];
				
				Map<String, String> compareMap = new HashMap<String, String>();
				for(int i=0, len=fileList.size(); i<len; i++){
					fileItem = fileList.get(i);
					
					file = new File(fileItem.get("file_url") + File.separator + fileItem.get("file_nm"));
					
					// 파일명 중복 처리
					if(compareMap.get((String) fileItem.get("real_file_nm")) != null){
						String tempFileName = (String) fileItem.get("real_file_nm");							
						String extension = tempFileName.substring(tempFileName.lastIndexOf("."));							    
						tempFileName = tempFileName.substring(0, tempFileName.lastIndexOf(extension)) +"_"+Integer.toString(i) +  extension;
						
						fileItem.put("real_file_nm", tempFileName);
					}
					
					compareMap.put((String) fileItem.get("real_file_nm") ,"exists");
					
					zen = new ZipEntry((String) fileItem.get("real_file_nm"));
					zos.putNextEntry(zen);
					fin = new FileInputStream(file);
					while ( (fLength = fin.read(buf, 0, buf.length)) >= 0 ){
						zos.write(buf, 0, fLength);
					}
					
					fin.close();
					zos.closeEntry();
				}
				zos.close();
			} catch (FileNotFoundException e) {
				 throw new ComBizException("파일을 압축하는 과정에 문제가 발생했습니다.");
			} catch (IOException e) {
				 throw new ComBizException("파일을 압축하는 과정에 문제가 발생했습니다.");
			} catch (Exception e) {
				 throw new ComBizException("파일을 압축하는 과정에 문제가 발생했습니다.");
			} finally {
				if(fin != null){
					try {
						fin.close();
					} catch (IOException e) {
						log.debug(e.getMessage());
					}
				}
				if(zos != null){
					try {
						zos.close();
					} catch (IOException e) {
						log.debug(e.getMessage());
					}
				}
				if(fout != null){
					try {
						fout.close();
					} catch (IOException e) {
						log.debug(e.getMessage());
					}
				}
			}

			try {
				ComFileUtil.fileDownloadWrapper(strDestDir+File.separator+fileName,fileName,request, response);
			}  catch (ComBizException e) {
				throw new ComBizException(e.getErrMsg());
			} finally {

			}
			
			//압축 파일 삭제
			deletePath(strDestDir+File.separator+fileName);
		}
		
		return new JSONDataView();	
	}
  
	public String deletePath(String fileUrl) {
		File file = new File(fileUrl);
		String result = "";
		
		if (file.exists()) {
			result = file.getAbsolutePath();
			if (!file.delete()) {
				result = "";
			}
		}
		return result;
	}
	
	/**
	  * 체크한 파일을 삭제한다.
	  *
	  * @param map
	  * @return ModelAndView
	  * @exception Exception
	*/
	@RequestMapping("/com/delete.do")
	public View delete(HttpServletRequest request, HttpServletResponse response, DataRequest dataRequest) throws Exception {
		ParameterGroup dsFile = dataRequest.getParameterGroup("dsUpload");

		if(dsFile != null){
			comfileService.deleteComFile(dsFile);
		}
		
		return new JSONDataView();
	}
	
	/**
	  * 그리드의 내용을 엑셀파일로 다운로드 한다.
	  *
	  * @param map
	  * @return ModelAndView
	  * @exception Exception
	*/
	@RequestMapping("/com/export/excel.do")
	public void exporteExcel(HttpServletRequest request, HttpServletResponse response, DataRequest dataRequest) throws IOException {
		String fileName = dataRequest.getParameter("fileName");
		
		String downloadFileName = fileName + ".xlsx";
		
		downloadFileName = this.encodingDownloadFileName(request, downloadFileName);
		
		response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		response.setHeader("Content-Disposition", "attachment;filename=\"" + downloadFileName + "\"");
		response.setHeader("Content-Transfer-Encoding", "binary");
		
		this.export(request, response, fileName, EXPORTTYPE.XLSX);
		//this.export(request, response, fileName, EXPORTTYPE.XLS);
	}
	
	/**
	  * 그리드의 내용을 엑셀파일로 생성한다.
	  *
	  * @param map
	  * @return ModelAndView
	  * @exception Exception
	*/
	private void export(HttpServletRequest request, HttpServletResponse response, String fileName, EXPORTTYPE type) throws IOException {
		request.setCharacterEncoding("utf-8");
		String newFileName = URLDecoder.decode(fileName, "utf-8");
		DataSource dataSource = JSONDataSourceBuilder.build(request, newFileName);
		OutputTarget outputTarget = new HttpResponseOutputTarget(response);
		
		ExporterFactory exporterFactory = ExporterFactory.getInstance();
		Exporter exporter = exporterFactory.getExporter(type);
		exporter.export(dataSource, outputTarget);
		
		response.flushBuffer();
	}
	
	/**
	  * 파일 이름을 인코딩한다.
	  *
	  * @param map
	  * @return ModelAndView
	  * @exception Exception
	*/
	private String encodingDownloadFileName(HttpServletRequest request, String downloadFileName) throws UnsupportedEncodingException {
		String userAgent = request.getHeader("User-Agent");
		
		if(userAgent.contains("MSIE") || userAgent.contains("Chrome") || (userAgent.contains("Windows") && userAgent.contains("Trident"))){
			downloadFileName = URLEncoder.encode(downloadFileName, "utf-8");
			downloadFileName = downloadFileName.replaceAll("\\+","%20");
        }
		
		return downloadFileName;
	}
	
    /**
	  * 업로드된 파일 리스트를 조회한다.
	  *
	  * @param DataRequest
	  * @return View
	  * @exception Exception    
	*/
	@RequestMapping("/com/saveFileDscr.do")
	public View saveFileDscr(DataRequest dataRequest) throws Exception {
		ParameterGroup dsUpload = dataRequest.getParameterGroup("dsUpload");
		
		comfileService.saveFileDscr(dataRequest);

		return new JSONDataView();
	}
	
	/**
	  * 파일을 다운로드한다.
	  *
	  * @param map
	  * @return ModelAndView
	  * @exception Exception
	*/
	@RequestMapping("/com/file/Download.do")
	public View comFileDownload(DataRequest dataRequest, HttpServletRequest request, HttpServletResponse response) throws Exception {
	   	ParameterGroup dmParamDown = dataRequest.getParameterGroup("dmParamDown");
		
	   	String fileUrl =  ComStringUtil.nullTostring(dmParamDown.getValue("file_url"));
		String realfileName =  ComStringUtil.nullTostring(dmParamDown.getValue("real_file_nm"));
			
		try {
			ComFileUtil.fileDownloadWrapper(fileUrl,realfileName,request, response);
		}  catch (ComBizException e) {
			throw new ComBizException(e.getErrMsg());
		} finally {
			
		}
		
		return new JSONDataView();
	}
}

package com.sino.CollectNmonFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.junit.Test;

import com.sino.model.MonitorDataVO;

public class ExcelUtil {

	public static String out2Excel(String basePath, String[] head0,
			String[] head1, List<MonitorDataVO> monitorList) {

		HSSFWorkbook wb = new HSSFWorkbook();
		HSSFSheet sheet = wb.createSheet("Nmon监控数据");
		sheet.setDefaultColumnWidth(10);

		// data style
		HSSFCellStyle dataStyle = wb.createCellStyle();
		dataStyle.setAlignment(HorizontalAlignment.CENTER);
		dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		dataStyle.setWrapText(true);
		
		HSSFRow row0 = sheet.createRow(0);
		HSSFCell cell0 = row0.createCell(0);
		cell0.setCellValue(head0[0]);
		cell0.setCellStyle(dataStyle);
		cell0 = row0.createCell(1);
		cell0.setCellValue(head0[1]);
		cell0.setCellStyle(dataStyle);
		cell0 = row0.createCell(6);
		cell0.setCellValue(head0[2]);
		cell0.setCellStyle(dataStyle);
		cell0 = row0.createCell(9);
		cell0.setCellValue(head0[3]);
		cell0.setCellStyle(dataStyle);
		cell0 = row0.createCell(11);
		cell0.setCellValue(head0[4]);
		cell0.setCellStyle(dataStyle);

		sheet.addMergedRegion(new CellRangeAddress(0, 1, 0, 0));
		sheet.addMergedRegion(new CellRangeAddress(0, 0, 1, 5));
		sheet.addMergedRegion(new CellRangeAddress(0, 0, 6, 8));
		sheet.addMergedRegion(new CellRangeAddress(0, 0, 9, 10));
		sheet.addMergedRegion(new CellRangeAddress(0, 0, 11, 14));

		HSSFRow row = sheet.createRow(1);
		for (int i = 0; i < head1.length; i++) {
			HSSFCell cell = row.createCell(i+1);
			cell.setCellValue(head1[i]);
			cell.setCellStyle(dataStyle);
		}

		for(int i=0;i<monitorList.size();i++){
			int j = 0;
			HSSFRow dataRow = sheet.createRow(i+2);
			HSSFCell dataCell = dataRow.createCell(j++);
			dataCell.setCellStyle(dataStyle);
			dataCell.setCellValue(monitorList.get(i).getHostIP());
			
			//CPU
			for(int k=0;k<monitorList.get(i).getCpuInfo().length;k++){
				dataCell = dataRow.createCell(j++);
				dataCell.setCellStyle(dataStyle);
				dataCell.setCellValue(StandardizeFloat(monitorList.get(i).getCpuInfo()[k]));
			}
			
			//DISK
			for(int k=0;k<monitorList.get(i).getDiskInfo().length;k++){
				dataCell = dataRow.createCell(j++);
				dataCell.setCellStyle(dataStyle);
				dataCell.setCellValue(StandardizeFloat(monitorList.get(i).getDiskInfo()[k]));
			}
			
			//net
			for(int k=0;k<2;k++){
				dataCell = dataRow.createCell(j++);
				dataCell.setCellStyle(dataStyle);
				dataCell.setCellValue(StandardizeFloat(monitorList.get(i).getNetInfo()[k]));
			}
			
			//mem
			for(int k=0;k<monitorList.get(i).getMemInfo().length;k++){
				dataCell = dataRow.createCell(j++);
				dataCell.setCellStyle(dataStyle);
				dataCell.setCellValue(StandardizeFloat(monitorList.get(i).getMemInfo()[k]));
			}
		}
		
		// IO operation
		OutputStream os = null;
		try {
			// 生成一个待压缩的文件夹downfile，待返回链接供前端下载
			File file = new File(basePath);
			if (!file.exists()) {
				file.mkdirs();
			}
			String exportPath = basePath + System.currentTimeMillis() + ".xls";
			os = new FileOutputStream(exportPath);
			wb.write(os);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				os.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		return "";
	}

	private static double StandardizeFloat(float num){
		BigDecimal temp = new BigDecimal(num);
		return temp.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
	}
	
	@Test
	public void testOut2Excel() {
		String head0[] = { "服务器地址", "CPU使用情况", "磁盘使用情况", "网络使用情况", "内存使用情况" };
		String head1[] = { "User%", "sys%", "wait%", "idle%", "CPU%",
				"Disk Read", "Disk Write", "IO/sec", "en0-reads", "en0.writes",
				"memtotal", "memUsed%", "swaptotal", "swapUsed%" };
		out2Excel("D:/1/", head0, head1, null);
		System.out.println("OK");
	}
}

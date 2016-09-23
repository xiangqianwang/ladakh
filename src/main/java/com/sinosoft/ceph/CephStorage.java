package com.sinosoft.ceph;

import java.io.IOException;
import java.util.Iterator;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinosoft.ceph.exception.CephException;

public class CephStorage {
	private static Logger logger = LoggerFactory.getLogger(CephStorage.class);
	
	private String restApiUrl;

	public CephStorage(String restApiUrl) {
		this.restApiUrl = restApiUrl;
	}
	
	public CephStatus getStatus() {
		CephStatus status = new CephStatus();
		
		// use javax.ws.rs.client if reasteasy-xxx-3.x is used
		Client client = ClientBuilder.newClient();		
		WebTarget target = client.target(restApiUrl).path("status");
		Builder builder = target.request(MediaType.APPLICATION_JSON);		
		Response response = builder.get();
		String message = response.readEntity(String.class);
		logger.debug(message);
 		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(message).get("output");
		} catch (JsonProcessingException e) {
			throw new CephException("解析数据出错。" + e.getMessage(), e);
		} catch (IOException e) {
			throw new CephException("解析数据出错。" + e.getMessage(), e);
		}

		status.setClusterId(null == rootNode.get("fsid") ? "" : rootNode.get("fsid").asText());
		
		JsonNode healthNode = rootNode.path("health");
		status.setOverallStatus(null == healthNode.get("overall_status") ? "" : healthNode.get("overall_status").asText());
		Iterator<JsonNode> summaryNodes = healthNode.get("summary").elements();
		if (summaryNodes.hasNext()) {
			JsonNode summaryNode = summaryNodes.next();
			status.setSummaryMessage(null == summaryNode.get("summary") ? "" : summaryNode.get("summary").asText());
		}

		JsonNode osdmapNode = rootNode.path("osdmap");
		status.setOsdCount(null == osdmapNode.get("osdmap").get("num_osds") ? -1 : osdmapNode.get("osdmap").get("num_osds").asInt());
		status.setOsdUpCount(null == osdmapNode.get("osdmap").get("num_up_osds") ? -1 : osdmapNode.get("osdmap").get("num_up_osds").asInt());
		status.setOsdInCount(null == osdmapNode.get("osdmap").get("num_in_osds") ? -1 : osdmapNode.get("osdmap").get("num_in_osds").asInt());
		
		
		JsonNode pgmapNode = rootNode.path("pgmap");
		status.setPgCount(null == pgmapNode.get("num_pgs") ? -1 : pgmapNode.get("num_pgs").asInt());
		status.setTotalSize(null == pgmapNode.get("bytes_total") ? -1 : (int)(pgmapNode.get("bytes_total").asLong() / 1024 / 1024 / 1024));
		status.setUsedSize(null == pgmapNode.get("bytes_used") ? -1 : (int)(pgmapNode.get("bytes_used").asLong() / 1024 / 1024 / 1024));
		status.setAvailableSize(null == pgmapNode.get("bytes_avail") ? -1 : (int)(pgmapNode.get("bytes_avail").asLong() / 1024 / 1024 / 1024));
		
		return status;
	}
}
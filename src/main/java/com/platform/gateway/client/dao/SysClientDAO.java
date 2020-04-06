package com.platform.gateway.client.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 *
 * 查询应用绑定的资源权限
 */
@Mapper
public interface SysClientDAO {

	 
	@Select("select * from oauth_client_details t where t.client_code = #{clientCode}")
	Map getClient(String clientCode);
	
	
	@Select("select * from oauth_client_details t where status=1 ")
	List<Map> findAll();

 
}

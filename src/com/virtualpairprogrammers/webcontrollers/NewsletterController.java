package com.virtualpairprogrammers.webcontrollers;

import java.io.IOException;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualpairprogrammers.rest.representations.CustomerCollectionRepresentation;

@Controller
public class NewsletterController 
{	
	@Autowired
	private OAuth2RestTemplate oauthTemplate;
	
	@RequestMapping("/build-newsletter")
	public ModelAndView displayAllCustomersOnWebPage()
	{
		return new ModelAndView("/newsletter.jsp");
	}
	@RequestMapping("/import")
	public ModelAndView muchBetterVersion()
	{
		CustomerCollectionRepresentation customers = new CustomerCollectionRepresentation();
		try
		{
			
			System.out.println("\n\n\n I am in hereeeeee \n\n\n");
			
			System.out.println(customers.getCustomers());
			 
			System.out.println("Template = "+oauthTemplate);
			
			
			customers = oauthTemplate.getForObject("http://localhost:8081/crm/rest/customers?fullDetails=true",CustomerCollectionRepresentation.class);
		


		}
		catch(HttpClientErrorException e)
		{
			System.out.println(e.getCause());
			System.out.println(e.getMessage());

		}

		return new ModelAndView("/importedContacts.jsp","customers",customers.getCustomers());	
	}
	
	
	
	
	public ModelAndView firstUglyVersion(@RequestParam("code") String code) throws JsonProcessingException, IOException
	{
		RestTemplate template = new RestTemplate();
		
		String credentials = "mailmonkey:somesecretkey";
		String encodedString = new String(Base64.encodeBase64(credentials.getBytes()));
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Basic " + encodedString);
			
		// TODO make this https
		String url = "http://localhost:8081/crm/oauth/token";
		
		MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
		params.add("code", code);
		params.add("grant_type", "authorization_code");
		params.add("redirect_uri", "http://localhost:8081/mailmonkey/import.html");
		params.add("client_id", "mailmonkey");
		
		HttpEntity request = new HttpEntity(params,headers);
		ResponseEntity<String> response = template.exchange(url, HttpMethod.POST,request, String.class);		
		
		// Leg 3: 
		url = "http://localhost:8081/crm/rest/customers?fullDetails=true";
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(response.getBody());
		String token = node.path("access_token").asText();
		

		HttpHeaders headersLeg3 = new HttpHeaders();
		headersLeg3.add("Authorization", "Bearer " + token);
		HttpEntity requestLeg3 = new HttpEntity(headersLeg3);
		
		ResponseEntity<CustomerCollectionRepresentation> customers = template.exchange(url, HttpMethod.GET, requestLeg3, CustomerCollectionRepresentation.class);
		
		return new ModelAndView("/importedContacts.jsp","customers",customers.getBody().getCustomers());
	}
}

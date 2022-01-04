
package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.client.RestTemplate;

public class TiingoService implements StockQuotesService {

  public static final String token = "128e629371cb3d5c219389fc96c8fa89fd5c2f02";

private RestTemplate restTemplate;

  protected TiingoService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }



  
  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
   
     String uriTemplate = "https://api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
          + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";

          String uri = uriTemplate.replace("$SYMBOL",symbol).replace("$APIKEY",token).replace("$STARTDATE",startDate.toString()).replace("$ENDDATE",endDate.toString());

          return uri;
}

private static ObjectMapper getObjectMapper() {
  ObjectMapper objectMapper = new ObjectMapper();
  objectMapper.registerModule(new JavaTimeModule());
  return objectMapper;
}

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to) throws JsonProcessingException,StockQuoteServiceException,RuntimeException {
    
    if(from.compareTo(to)>=0)
     {
       throw new RuntimeException(); 
     }
     List<Candle> stocksStartDateToEndDate = new ArrayList<Candle>();
     try {
      String url = buildUri(symbol, from, to);
      String stocks = restTemplate.getForObject(url,String.class);
      ObjectMapper objectMapper = getObjectMapper();
      TiingoCandle[] stocksStartDateToEndDateArray = objectMapper.readValue(stocks,TiingoCandle[].class);
      
      if (stocksStartDateToEndDate != null) {
          stocksStartDateToEndDate = Arrays.asList(stocksStartDateToEndDateArray);
       } else {
       stocksStartDateToEndDate = Arrays.asList(new TiingoCandle[0]);
       }
     } catch (NullPointerException e) {
       throw new StockQuoteServiceException("error occured in the response from Tiingo API", e.getCause());
     }
     return stocksStartDateToEndDate;
    }


  // TODO: CRIO_TASK_MODULE_EXCEPTIONS
  //  1. Update the method signature to match the signature change in the interface.
  //     Start throwing new StockQuoteServiceException when you get some invalid response from
  //     Tiingo, or if Tiingo returns empty results for whatever reason, or you encounter
  //     a runtime exception during Json parsing.
  //  2. Make sure that the exception propagates all the way from
  //     PortfolioManager#calculateAnnualisedReturns so that the external user's of our API
  //     are able to explicitly handle this exception upfront.

  //CHECKSTYLE:OFF








}

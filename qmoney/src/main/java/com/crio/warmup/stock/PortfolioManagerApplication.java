
package com.crio.warmup.stock;


import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TotalReturnsDto;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TotalReturnsDto;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.text.AbstractDocument.Content;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;


public class PortfolioManagerApplication {


  public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {
      Files.readAllLines(resolveFileFromResources(args[0]).toPath());
      ObjectMapper m = getObjectMapper();
      PortfolioTrade pftrades[] = m.readValue(resolveFileFromResources(args[0]),PortfolioTrade[].class);
      List<String> finalAns = new ArrayList<String>();
      for(PortfolioTrade pftrade : pftrades)
      {
        finalAns.add(pftrade.getSymbol());
      }
     return finalAns;
  }

  public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
      throws IOException, URISyntaxException {
        
    Files.readAllLines(resolveFileFromResources(args[0]).toPath());
    ObjectMapper m = getObjectMapper();
    PortfolioTrade pftrades[] = m.readValue(resolveFileFromResources(args[0]),PortfolioTrade[].class);
    List<AnnualizedReturn> annualizedreturnList = new ArrayList<AnnualizedReturn>();

    String token = "128e629371cb3d5c219389fc96c8fa89fd5c2f02";
    RestTemplate rt = new RestTemplate();
    for(PortfolioTrade pftrade : pftrades)
    {
      String path = "https://api.tiingo.com/tiingo/daily/"+pftrade.getSymbol()+"/prices?startDate="+pftrade.getPurchaseDate()+"&endDate="+args[1]+"&token="+token;
      TiingoCandle[] tc = rt.getForObject(path,TiingoCandle[].class);
      int l = tc.length;
      Double buy_value = tc[0].getOpen();
      Double sell_value = tc[l-1].getClose();

      AnnualizedReturn ar = calculateAnnualizedReturns(tc[l-1].getDate(),
      pftrade,buy_value,sell_value);

      annualizedreturnList.add(ar);
    }
    Collections.sort(annualizedreturnList, new Comparator<AnnualizedReturn>() {
      @Override
      public int compare(AnnualizedReturn ar1, AnnualizedReturn ar2) {
           if(ar1.getAnnualizedReturn()<ar2.getAnnualizedReturn())
           {
             return 1;
           }
           else
           {
             return -1;
           }
      }
  });
     return annualizedreturnList;
  }

  public static RestTemplate restTemplate = new RestTemplate();
  public static PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager(restTemplate);
  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
      PortfolioTrade trade, Double buyPrice, Double sellPrice) {
        Double total_num_years = trade.getPurchaseDate().until(endDate, ChronoUnit.DAYS) / 365.24; 
        Double totalReturns = (sellPrice-buyPrice)/buyPrice;
        Double annualizedReturn = Math.pow((1+totalReturns),(1/total_num_years))-1;
      return new AnnualizedReturn(trade.getSymbol(),annualizedReturn,totalReturns);
  }

  private static void printJsonObject(Object object) throws IOException {
    Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
    ObjectMapper mapper = new ObjectMapper();
    logger.info(mapper.writeValueAsString(object));
  }

  private static File resolveFileFromResources(String filename) throws URISyntaxException {
    return Paths.get(
        Thread.currentThread().getContextClassLoader().getResource(filename).toURI()).toFile();
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  public static List<String> debugOutputs() {

     String valueOfArgument0 = "trades.json";
     String resultOfResolveFilePathArgs0 = "trades.json";
     String toStringOfObjectMapper = "ObjectMapper";
     String functionNameFromTestFileInStackTrace = "mainReadFile";
     String lineNumberFromTestFileInStackTrace = "";


    return Arrays.asList(new String[]{valueOfArgument0, resultOfResolveFilePathArgs0,
        toStringOfObjectMapper, functionNameFromTestFileInStackTrace,
        lineNumberFromTestFileInStackTrace});
  }

  public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {
    Files.readAllLines(resolveFileFromResources(args[0]).toPath());
    ObjectMapper m = getObjectMapper();
    PortfolioTrade pftrades[] = m.readValue(resolveFileFromResources(args[0]),PortfolioTrade[].class);
    List<Candle> finalAns = new ArrayList<Candle>();
    Map<Double,String> map = new TreeMap<Double,String>();
    String token = "128e629371cb3d5c219389fc96c8fa89fd5c2f02";
    RestTemplate rt = new RestTemplate();
    for(PortfolioTrade pftrade : pftrades)
    {
      String path = "https://api.tiingo.com/tiingo/daily/"+pftrade.getSymbol()+"/prices?startDate=2019-12-01&endDate="+args[1]+"&token="+token;
      TiingoCandle[] tc = rt.getForObject(path,TiingoCandle[].class);
      //System.out.println("working for - "+pftrade.getSymbol());
      
      int l = tc.length;
      
        finalAns.add(tc[l-1]);
        map.put(tc[l-1].getClose(),pftrade.getSymbol());
    }

    List<String> returnedAns = new ArrayList<String>();
    
    // System.out.println(
    //   "TreeMap: " + map);

        for (Map.Entry<Double, String> e : map.entrySet())
            {
              returnedAns.add(e.getValue());
            }
                              

     return returnedAns;
  }

  private static String readFileAsString(String filename) throws URISyntaxException, IOException { 
  
    return new String(Files.readAllBytes(resolveFileFromResources(filename).toPath()), "UTF-8"); 
    }
   

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
      throws Exception {
       String file = args[0];
       LocalDate endDate = LocalDate.parse(args[1]);
       String contents = readFileAsString(file);
       ObjectMapper objectMapper = getObjectMapper();
        PortfolioTrade[] portfolioTrades = objectMapper.readValue(contents, PortfolioTrade[].class);
       return portfolioManager.calculateAnnualizedReturn(Arrays.asList(portfolioTrades), endDate);
  }



















  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());
    //printJsonObject(mainReadFile(args));
    //printJsonObject(mainReadQuotes(args));
    //printJsonObject(mainCalculateSingleReturn(args));
    printJsonObject(mainCalculateReturnsAfterRefactor(args));

  }
}


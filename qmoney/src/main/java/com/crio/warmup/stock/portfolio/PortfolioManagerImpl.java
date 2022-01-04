
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.management.RuntimeErrorException;

import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {

RestTemplate restTemplate;
private StockQuotesService stockQuotesService;



  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  protected PortfolioManagerImpl(StockQuotesService stockQuotesService) {
    this.stockQuotesService = stockQuotesService;
  }

  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }




  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException, StockQuoteServiceException {
        return stockQuotesService.getStockQuote(symbol, from, to);
  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    String token = "128e629371cb3d5c219389fc96c8fa89fd5c2f02";
     String uriTemplate = "https://api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
          + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";

          String uri = uriTemplate.replace("$SYMBOL",symbol).replace("$APIKEY",token).replace("$STARTDATE",startDate.toString()).replace("$ENDDATE",endDate.toString());

          return uri;
}

public AnnualizedReturn getAnnualizedReturn(PortfolioTrade trade,LocalDate endLocalDate)
    throws StockQuoteServiceException
{
  AnnualizedReturn annualizedReturn;
  String symbol = trade.getSymbol();
  LocalDate startLocalDate = trade.getPurchaseDate();

  try
  {
    List<Candle> stocksStartToEndDate;
    stocksStartToEndDate = getStockQuote(symbol, startLocalDate, endLocalDate);

    Candle stockStartDate = stocksStartToEndDate.get(0);
    Candle stockLatest = stocksStartToEndDate.get(stocksStartToEndDate.size()-1);
    Double buyPrice = stockStartDate.getOpen();
    Double sellPrice = stockLatest.getClose();

    Double totalReturn = (sellPrice - buyPrice)/buyPrice;
    Double numYears = (double) ChronoUnit.DAYS.between(startLocalDate,endLocalDate)/365;

    Double annualizedReturns = Math.pow((1+totalReturn),(1/numYears))-1;
    annualizedReturn = new AnnualizedReturn(symbol, annualizedReturns, totalReturn);
  }
  catch(JsonProcessingException e)
  {
    annualizedReturn = new AnnualizedReturn(symbol,Double.NaN, Double.NaN);
  }
  return annualizedReturn;
}


  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades, LocalDate endDate)
      throws StockQuoteServiceException {
    AnnualizedReturn annualizedReturn;
    List<AnnualizedReturn> annualizedReturns = new ArrayList<AnnualizedReturn>();
    for(int i=0;i<portfolioTrades.size();i++)
    {
      annualizedReturn = getAnnualizedReturn(portfolioTrades.get(i),endDate);
      annualizedReturns.add(annualizedReturn);
    }

    Comparator<AnnualizedReturn> SortByAnnReturn = Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
    Collections.sort(annualizedReturns,SortByAnnReturn);

      return annualizedReturns;

  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturnParallel(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate, int numThreads) throws InterruptedException, StockQuoteServiceException {
        List <Future<AnnualizedReturn>> futureReturnsList = new ArrayList<Future<AnnualizedReturn>>();
        final ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        for(int i=0;i<portfolioTrades.size();i++)
        {
          PortfolioTrade trade = portfolioTrades.get(i);
          Callable<AnnualizedReturn> callableTask = () -> {
            return getAnnualizedReturn(trade, endDate);
          };
          Future<AnnualizedReturn> futureReturns = pool.submit(callableTask);
          futureReturnsList.add(futureReturns);
        }
        List<AnnualizedReturn> annualizedReturns = new ArrayList<AnnualizedReturn>();
          for(int i=0;i<portfolioTrades.size();i++)
          {
            Future<AnnualizedReturn> futureReturns = futureReturnsList.get(i);
            try{
              AnnualizedReturn returns = futureReturns.get();
              annualizedReturns.add(returns); 
            }catch(Exception e)
            {
              throw new StockQuoteServiceException("Error on calling API",e);
            }
          }
          Collections.sort(annualizedReturns,getComparator());
          return annualizedReturns;

  }


}

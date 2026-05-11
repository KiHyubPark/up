package com.clone.up.domain.indicator.service;

import com.clone.up.domain.candle.entity.Candle;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.Num;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class IndicatorCalculationService {

    /**
     * Candle 목록으로 TA4J BarSeries를 빌드합니다.
     * candleTime은 UTC 기준이며 ZoneOffset.UTC로 변환합니다.
     */
    public BarSeries buildBarSeries(String seriesName, List<Candle> candles) {
        BarSeries series = new BaseBarSeriesBuilder().withName(seriesName).build();
        for (Candle c : candles) {
            series.addBar(
                    c.getCandleTime().atZone(ZoneOffset.UTC),
                    c.getOpenPrice(),
                    c.getHighPrice(),
                    c.getLowPrice(),
                    c.getClosePrice(),
                    c.getVolume()
            );
        }
        return series;
    }

    public List<Double> sma(BarSeries series, int period) {
        SMAIndicator sma = new SMAIndicator(new ClosePriceIndicator(series), period);
        return toList(sma, series.getBarCount());
    }

    public List<Double> ema(BarSeries series, int period) {
        EMAIndicator ema = new EMAIndicator(new ClosePriceIndicator(series), period);
        return toList(ema, series.getBarCount());
    }

    public List<Double> rsi(BarSeries series, int period) {
        RSIIndicator rsi = new RSIIndicator(new ClosePriceIndicator(series), period);
        return toList(rsi, series.getBarCount());
    }

    public List<Double> macd(BarSeries series, int shortPeriod, int longPeriod) {
        MACDIndicator macd = new MACDIndicator(
                new ClosePriceIndicator(series), shortPeriod, longPeriod);
        return toList(macd, series.getBarCount());
    }

    public List<Double> macdSignal(BarSeries series, int shortPeriod, int longPeriod, int signalPeriod) {
        MACDIndicator macd = new MACDIndicator(
                new ClosePriceIndicator(series), shortPeriod, longPeriod);
        EMAIndicator signal = new EMAIndicator(macd, signalPeriod);
        return toList(signal, series.getBarCount());
    }

    public BollingerBands bollingerBands(BarSeries series, int period, double multiplier) {
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        SMAIndicator sma = new SMAIndicator(close, period);
        StandardDeviationIndicator stdDev = new StandardDeviationIndicator(close, period);

        BollingerBandsMiddleIndicator mid = new BollingerBandsMiddleIndicator(sma);
        BollingerBandsUpperIndicator upper = new BollingerBandsUpperIndicator(
                mid, stdDev, series.numOf(multiplier));
        BollingerBandsLowerIndicator lower = new BollingerBandsLowerIndicator(
                mid, stdDev, series.numOf(multiplier));

        return new BollingerBands(
                toList(upper, series.getBarCount()),
                toList(mid, series.getBarCount()),
                toList(lower, series.getBarCount())
        );
    }

    private List<Double> toList(org.ta4j.core.Indicator<Num> indicator, int barCount) {
        List<Double> result = new ArrayList<>(barCount);
        for (int i = 0; i < barCount; i++) {
            result.add(indicator.getValue(i).doubleValue());
        }
        return result;
    }

    public record BollingerBands(
            List<Double> upper,
            List<Double> middle,
            List<Double> lower
    ) {
    }
}

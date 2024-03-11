package shx.cotacaodolar.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import shx.cotacaodolar.model.Moeda;
import shx.cotacaodolar.model.Periodo;



@Service
public class MoedaService {

	// o formato da data que o método recebe é "MM-dd-yyyy"
    public List<Moeda> getCotacoesPeriodo(String startDate, String endDate) throws IOException, MalformedURLException, ParseException{
        Periodo periodo = new Periodo(startDate, endDate);

        String urlString = "https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata/CotacaoDolarPeriodo(dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)?%40dataInicial='" + periodo.getDataInicial() + "'&%40dataFinalCotacao='" + periodo.getDataFinal() + "'&%24format=json&%24skip=0&%24top=" + periodo.getDiasEntreAsDatasMaisUm();

        URL url = new URL(urlString);
        HttpURLConnection request = (HttpURLConnection)url.openConnection();
        request.connect();

        JsonElement response = JsonParser.parseReader(new InputStreamReader((InputStream)request.getContent()));
        JsonObject rootObj = response.getAsJsonObject();
        JsonArray cotacoesArray = rootObj.getAsJsonArray("value");

        List<Moeda> moedasLista = new ArrayList<Moeda>();

        for(JsonElement obj : cotacoesArray){
            Moeda moedaRef = new Moeda();
            Date data = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(obj.getAsJsonObject().get("dataHoraCotacao").getAsString());

            moedaRef.preco = obj.getAsJsonObject().get("cotacaoCompra").getAsDouble();
            moedaRef.data = new SimpleDateFormat("dd/MM/yyyy").format(data);
            moedaRef.hora = new SimpleDateFormat("HH:mm:ss").format(data);
            moedasLista.add(moedaRef);
        }
        return moedasLista;
    }


    public Moeda getCotacaoAtual() throws IOException, ParseException {
        //Date date = Date.from(Instant.now().plus(Duration.ofDays(1))); // para testes com dias futuros
        Date date = Date.from(Instant.now());
        String strDate = new SimpleDateFormat("MM-dd-yyyy").format(date);
        List<Moeda> cotacoesPeriodo= getCotacoesPeriodo(strDate, strDate);

        // no caso de se considerar a cotação atual como sendo a cotação do último fechamento
        // como no caso da consulta ser feita aos finais de semana.
        while(cotacoesPeriodo.isEmpty()){
            date = Date.from(date.toInstant().minus(Duration.ofDays(1)));
            strDate = new SimpleDateFormat("MM-dd-yyyy").format(date);
            cotacoesPeriodo = getCotacoesPeriodo(strDate, strDate);
        }

        return cotacoesPeriodo.get(0);

    }

    public List<Moeda> getCotacoesMenoresAtual(String startDate, String endDate) throws IOException, ParseException {
        List<Moeda> listaMoedas = getCotacoesPeriodo(startDate, endDate);
        Moeda cotacaoAtual = getCotacaoAtual();
        return listaMoedas.stream().filter(moeda -> moeda.preco < cotacaoAtual.preco).toList();
    }
}

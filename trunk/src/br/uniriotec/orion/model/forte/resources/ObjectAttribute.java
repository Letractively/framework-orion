/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package br.uniriotec.orion.model.forte.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Felipe
 */
public class ObjectAttribute extends ForteResource{
    private String refConceito;
    private Map<String, List<String>> atributos;

    public ObjectAttribute(){
        atributos = new HashMap<String, List<String>>();
    }

    public Map<String, List<String>> getAtributos() {
        return atributos;
    }

    public void setAtributos(Map<String, List<String>> atributos) {
        this.atributos = atributos;
    }

    public String getRefConceito() {
        return refConceito;
    }

    public void setRefConceito(String refConceito) {
        this.refConceito = refConceito;
    }

    /**
     * Adiciona um atributo à lista de atributos caso o parametro passado ainda
     * não faça parte do conjunto.
     * 
     * @param att
     */
    public void addAtributo(String att){
        if(atributos.containsKey(att) == false){
            atributos.put(att, new ArrayList<String>());
        }
    }

    /**
     * Adiciona um novo valor à um determinado atributo da lista de atributos
     *
     * @param atributo
     * @param valor
     * @throws IllegalArgumentException
     */
    public void addValorAtributo(String atributo, String valor){
        if(atributos.containsKey(atributo) && atributos.get(atributo).contains(valor) == false){
            atributos.get(atributo).add(valor);
        }else{
            throw new IllegalArgumentException(
                    "Verifique se o atributo já foi inserido e se o valor não é repetido.");
        }
    }

    @Override
    public String toString(){
        String objAtt = refConceito + "([";
        for(String idx : atributos.keySet()){
            objAtt += idx + "([";
            List<String> listaValores = atributos.get(idx);
            if(listaValores.isEmpty() == false){
                for(String valor : listaValores){
                    objAtt += valor + ",";
                }
                objAtt = objAtt.substring(0, objAtt.length()-1);
            }
            objAtt += "]),";
        }
        objAtt = objAtt.substring(0, objAtt.length()-1);
        objAtt += "])";
        return objAtt;
    }
}

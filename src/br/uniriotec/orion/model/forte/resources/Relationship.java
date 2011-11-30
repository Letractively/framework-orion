
package br.uniriotec.orion.model.forte.resources;



/** Classe representante de um relacionamento em clausula de Horn.
 *
 * @author Felipe
 */
public class Relationship extends ForteResource {
    private String nomeRelacionamento;
    private String primeiroTermo;
    private String segundoTermo;

    @Override
    public String toString(){
        return nomeRelacionamento + "(A,B) :- example(" + nomeRelacionamento + "(A,B))";
    }
    
    /**
     * retorna todos as string que representam relacionamentos para o FORTE.
     * Um relacionamento é baseado em um ObjetctProperty da ontologia, que tem apenas um domain,
     * mas pode possuir multiplos Ranges, desta forma, cada regra gerada representa um par
     * Range+Domain.
     * 
     * @return List<String>
     */
    public String getRelacionamentos(){
        return nomeRelacionamento + "(A,B) :- " + primeiroTermo + "(A), " + segundoTermo + "(B)";
    }

    public String getNome() {
        return nomeRelacionamento;
    }

    public void setNome(String nome) {
        this.nomeRelacionamento = nome;
    }

    public String getPrimeiroTermo() {
        return primeiroTermo;
    }

    public void setPrimeiroTermo(String primeiroTermo) {
        this.primeiroTermo = primeiroTermo;
    }

    public String getSegundoTermo() {
        return segundoTermo;
    }

    public void setSegundoTermo(String segundoTermo) {
        this.segundoTermo = segundoTermo;
    }
}

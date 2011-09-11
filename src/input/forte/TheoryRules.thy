ex_Jogador(A) :- jogador(A), fdt:naotitular(A).
apelido_Time(A) :- fdt:apelido(A), fdt:naoapelido_Torcida(A), fdt:refereseTime(A,B), fdt:Time(B).
presidente(A) :- funcionario(A), fdt:naotecnico(A), fdt:comandaClube(A,B), fdt:Time(B).
membro_Comissao_Tecnica(A) :- funcionario(A), fdt:naotecnico(A), fdt:naopresidente(A), fdt:trabalhaEm(A,B), fdt:Time(B).
funcionario(A) :- fdt:pessoa(A).
jogador(A) :- fdt:pessoa(A), fdt:jogaEmTime(A,B), fdt:Time(B).

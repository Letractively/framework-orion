apelido_Time(A) :- fdt:apelido_Time_refereseTime(A,B), time(B).
presidente(A) :- fdt:presidente_comandaClube(A,B), time(B).
torcida(A) :- fdt:torcida_TorcePorTime(A,B), time(B).
time(A) :- true.
apelido(A) :- apelido_Pessoa(A). 
apelido(A) :-  apelido_Time(A). 
apelido(A) :-  apelido_Torcida(A).
funcionario(A) :- presidente(A). 
funcionario(A) :-  membro_Comissao_Tecnica(A). 
funcionario(A) :-  tecnico(A).
apelido_Torcida(A) :- fdt:apelido_Torcida_refereseTorcida(A,B), torcida(B).
membro_Comissao_Tecnica(A) :- fdt:membro_Comissao_Tecnica_trabalhaEm(A,B), time(B).
mascote(A) :- fdt:mascote_simboliza(A,B), time(B).
tecnico(A) :- fdt:tecnico_trabalhaEm(A,B), time(B).
estadio(A) :- fdt:estadio_pertenceTime(A,B), time(B).
jogador_Ativo(A) :- fdt:jogador_Ativo_jogaEmTime(A,B), time(B).
jogador_Aposentado(A) :- fdt:jogador_Aposentado_jogaEmTime(A,B), time(B).
patrocinador(A) :- fdt:patrocinador_patrocinaTime(A,B), time(B).
torcedor(A) :- fdt:torcedor_TorcePorTime(A,B), time(B).
pessoa(A) :- funcionario(A). 
pessoa(A) :-  jogador(A). 
pessoa(A) :-  torcedor(A).
jogador(A) :- jogador_Ativo(A). 
jogador(A) :-  jogador_Aposentado(A).
apelido_Pessoa(A) :- fdt:apelido_Pessoa_referesePessoa(A,B), pessoa(B).

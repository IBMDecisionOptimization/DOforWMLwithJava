int BigM = 100000000000000;


int Capacidade = 600000000;

int leadtime = 1;

tuple Sku {
  key int sku_id;
}
  {Sku} sku = ...;
  
  
tuple Client {
  key int client_id;
} 
  {Client} client = ...;
  
tuple Time {
  key int time_id;
}
  {Time} time = ...;
  
tuple Warehouse {
  key int time_id;
  key int sku_id;
  int retorno;
}
  {Warehouse} warehouse = ...;


tuple Negotiation {
  key int time_id;
  key int client_id;
  key int sku_id;
  float demand;
  float cost_sale;
}
  {Negotiation} negotiation = ...;
  
tuple Product {
  key int time_id;
  key int sku_id;
  float cost_buy;
  float safety_stock;
}
  {Product} product = ...;
  
tuple Stock_inicial {
  key int sku_id;
  float inicial;
}
  {Stock_inicial} stock_inicial = ...;
  
//VARIÁVEIS DE DECISÃO
//quantidade de produto comprado
dvar int+ compra[product];

//quantidade de produto vendido
dvar int+ venda[negotiation];

//estoque
dvar int+ saldo_final[sku][time];

//se existe uma compra de s em t
dvar boolean exist_compra[sku][time];

//se existe venda de s em t
dvar boolean exist_venda[sku][time];

minimize
  sum (p in product)compra[p] * p.cost_buy +
  sum (n in negotiation)venda[n] * n.cost_sale;
  
subject to {

//1. Stock
forall (t in time: t.time_id > 1)
  forall (s in sku)
    forall (w in warehouse: w.time_id == t.time_id && w.sku_id == s.sku_id)
      forall (p in product: p.time_id == t.time_id && p.sku_id == s.sku_id)
      Stock:
      saldo_final[s][t] == saldo_final[s][<t.time_id - 1>] + compra[p] - sum(n in negotiation: n.time_id == t.time_id && n.sku_id == s.sku_id)venda[n] + w.retorno;
 
//2. Initial inventory
forall (t in time: t.time_id == 1)
  forall (s in sku)
    forall (w in warehouse: w.time_id == t.time_id && w.sku_id == s.sku_id)
      forall (i in stock_inicial: i.sku_id == s.sku_id)
        Initial_inventory:
        saldo_final[s][t] == i.inicial;
        
//3. safety_stock
forall (t in time)
  forall (s in sku)
    forall (p in product: p.sku_id == s.sku_id && p.time_id == t.time_id)
      Safety_stock:
      saldo_final[s][t] >= p.safety_stock;
    
//4. Capacity
forall (t in time)
  Capacity:
  sum (s in sku)saldo_final[s][t] <= Capacidade;
  
//5. Venda e Estoque
forall (t in time: t.time_id > 1)
  forall (t1 in time: t1.time_id == 1)
  forall (s in sku)
    forall (i in stock_inicial: i.sku_id == s.sku_id)
    Venda_Estoque:
    if (t.time_id == 2){
    sum (n in negotiation: n.time_id == t.time_id && n.sku_id == s.sku_id)venda[n] <= saldo_final[s][t1];    
    }
    else {
    sum (n in negotiation: n.time_id == t.time_id && n.sku_id == s.sku_id)venda[n] <= saldo_final[s][<t.time_id - 1>];    
    }
        
//6. Demanda
forall (t in time)
forall (n in negotiation: n.time_id == t.time_id)
  Demanda:
  venda[n]  == n.demand; 
  
//7. LeadTime
forall (s in sku)
  forall (t1 in time)
    LeadTime:
    sum(t in time: t.time_id >= 1 && t.time_id <= t1.time_id - leadtime)exist_compra[s][t] <= BigM * exist_venda[s][t1];
    
//8.Existência
forall (s in sku)
  forall (t in time)
    forall (p in product: p.time_id == t.time_id && p.sku_id == s.sku_id)
      Existencia_compra:
      compra[p] <= BigM * exist_compra[s][t];
      
forall (s in sku)
  forall (t in time)
      Existencia_venda:
      sum (n in negotiation: n.time_id == t.time_id && n.sku_id == s.sku_id)venda[n] <= BigM * exist_venda[s][t]; 

}

tuple solution_estoque {
    int Mes;
    int Sku;
    int Estoque_final;
}
{solution_estoque} Solution_Estoque = {<t.time_id,s.sku_id,saldo_final[s][t]> |t in time, s in sku};
  
tuple solution_venda {
    int Mes;
    int Sku;
    int Cliente;
    int Venda; 
}
{solution_venda} Solution_Venda = {<n.time_id, n.sku_id, n.client_id, venda[n] > |n in negotiation};

tuple solution_compra {
    int Mes;
    int Sku;
    int Compra; 
}
{solution_compra} Solution_Compra = {<p.time_id, p.sku_id, compra[p] > |p in product};

tuple solution_retorno {
    int Mes;
    int Sku;
    int Retorno; 
}
{solution_retorno} Solution_Retorno = {<w.time_id, w.sku_id, w.retorno> |w in warehouse};
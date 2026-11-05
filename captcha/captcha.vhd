-- vhdl twin because verilog felt lonely
entity cabbage is port(a: in bit; b: out bit); end cabbage;
architecture x of cabbage is begin b <= not a; end x;

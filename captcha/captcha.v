// captcha verifier in hardware bc why not
module captcha(input clk, input [7:0] ans, output reg ok);
always @(posedge clk) ok <= (ans == 8'd42); // the answer is always 42
endmodule

$ ->
  ws = new WebSocket $("body").data("ws-url")
  ws.onmessage = (event) ->
    message = JSON.parse event.data
    switch message.type
      when "addStock"
            addStockQuoteCard(message)
            console.log(message)
      else
        console.log(message)

  $("#addsymbolform").submit (event) ->
    event.preventDefault()
    ws.send(JSON.stringify({symbol: $("#addsymboltext").val()}))
    # reset the form
    $("#addsymboltext").val("")

  $("#resetSymbols").submit (event) ->
      event.preventDefault()
      ws.send(JSON.stringify({symbol: "resetAllSymbols"}))
      # reset the form
      $("#addsymboltext").val("")


removeStock = (container) ->
  if (container.hasClass("card-holder"))
    container.remove()
    ws.send(JSON.stringify({symbol: $("#addsymboltext").val()}))

addStockQuoteCard = (message) ->
  if ($("#" + message.symbol).size() > 0)
    $("#" + message.symbol+"-price").html("$ " +message.price)
  else
   symbol = $("<div>").addClass("symbol").prop("id", message.symbol).text(message.symbol)
   price = $("<div>").addClass("price").prop("id", message.symbol+"-price").text("$ " +message.price)
   remove = $("<div>").addClass("remove").text("Remove")
   card = $("<div>").addClass("card").prop("id", message.symbol).append(symbol, price, remove)
   cardHolder = $("<div>").addClass("card-holder").prop("id", message.symbol).append(card).click (event) ->
     removeStock($(this))

   $("#stocks").prepend(cardHolder)

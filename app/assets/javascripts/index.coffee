$ ->
  ws = new WebSocket $("body").data("ws-url")
  ws.onmessage = (event) ->
    message = JSON.parse event.data
    switch message.type
      when "stockupdate"
            populateStockHistory(message)
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
      ws.send(JSON.stringify({symbol: $("#addsymboltext").val()}))
      # reset the form
      $("#addsymboltext").val("")


removeStock = (container) ->
  if (container.hasClass("card-holder"))
    container.removeClass("card-holder")



populateStockHistory = (message) ->
  symbol = $("<div>").addClass("symbol").prop("id", message.symbol).text(message.symbol)
  price =$("<div>").addClass("price").text("$ " +message.price)
  remove =$("<div>").addClass("remove").text("Remove")
  card = $("<div>").addClass("card").prop("id", message.symbol).append(symbol, price, remove)
  cardHolder = $("<div>").addClass("card-holder").prop("id", message.symbol).append(card).click (event) ->
    removeStock($(this))


  $("#stocks").prepend(cardHolder)

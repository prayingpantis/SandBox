function fetchHistory() {
  var sheet = SpreadsheetApp.getActiveSpreadsheet();
  
  var mapping_sheet = sheet.getSheets()[1];
  var mapping = mapping_sheet.getDataRange().getValues();
  var symmap = {}; //symbol mapping
  for (var i = 1; i < mapping.length; i++){
    symmap[mapping[i][0]] = mapping[i][1];
  }
  
  var history_sheet = sheet.getSheets()[0];
  var history = history_sheet.getDataRange().getValues();
  
  var import_sheet = sheet.getSheets()[2];
  var import = import_sheet.getDataRange().getValues();
  
  //Get last transaction for search string
  var last_date = history[history.length-1][2];
  Logger.log(last_date);
  var last_date_string = last_date.getFullYear() + "/" + (last_date.getMonth() + 1) + "/" + last_date.getDate();
  var search_string = 'from:info@gt247.com subject:"confirmation of easyequities transaction" is:unread  after:' + last_date_string;
  Logger.log(search_string);
  
  //Wise man say, stock market is like thermodynamics: can't win, can't break even, can't stop
  var threads = GmailApp.search(search_string);
  for (var i = threads.length; i --> 0; ){ //The fucking hotness backwards loop
    var thread = threads[i];
    var messages = thread.getMessages();
    for (var j = 0; j < messages.length; j++){
      var message = messages[j];
      //If the message is read we will ignore it
      if (message.isUnread() == false){
        Logger.log("Skipping read message: " + message.getDate());
        continue;
      }
      message.markRead();
      //Process the message
      var body = message.getBody();
      body = body.replace(/[\r\n]+/g, "NL");
      var name = /Images\/Stocks.*?465560;?">([^<]+)/.exec(body);
      var shares = /Shares.*?FSRs.*?right;?">\(?(\d+)\)?.*?12px;?">\(?\.(\d+)\)?/.exec(body);
      var price = /Trade Price:.*?left;?">([\d,.]+)/.exec(body);
      var buy = /header-(buy|sell)\.jpg/.exec(body);
      var commission = /(?:TOTAL TRANSACTION COST|LESS COSTS).*?center;?">([\d.]+)/.exec(body);
      if (name == null || shares == null || price == null || buy == null || commission == null){
        Logger.log([name,shares,price,buy,commission]);
        continue;
      }
      var data = {name: name[1], 
                  shares: shares[1], 
                  fsr: shares[2], 
                  price: price[1].replace(/,/g,""),
                  type: buy[1],
                  commission: commission[1],
                  date: message.getDate()
                 };
      Logger.log(data);
      history.push(
                   [
                    "JSE:" + symmap[data.name],
                    data.shares*1.0 + data.fsr/10000,
                    data.date,
                    data.price,
                    data.commission,
                    data.type
                   ]
                  );
      import.push(
                   [
                    "JSE:" + symmap[data.name],
                    data.shares*1.0 + data.fsr/10000,
                    data.date,
                    data.price,
                    data.commission,
                    data.type
                   ]
                  );
    }
  }
  
  Logger.log(history);
  history_sheet.getRange("A1:F" + history.length).setValues(history);
  import_sheet.getRange("A1:F" + import.length).setValues(import);
}

function fetchDeposits() {
  var sheet = SpreadsheetApp.getActiveSpreadsheet();
  
  var deposit_sheet = sheet.getSheets()[3];
  var deposit = deposit_sheet.getDataRange().getValues();
  
  //get deposit / withdrawal threads
  var search_string = 'from:info@gt247.com subject:"EasyEquities Deposit" OR subject:"EasyEquities Credit Card Payment" is:unread';
  Logger.log(search_string);
  
  //The lack of money is the root of all evil.
  var threads = GmailApp.search(search_string);
  
  //Regex to match deopist amount in headings
  var regExAmount = /(\R?(:?\d+,?.?)+)/;
  
  for (var i = threads.length; i --> 0; ) {
    // get all messages in a given thread
    var messages = threads[i].getMessages();
    
    // iterate over each message
    for (var j = 0; j < messages.length; j++) {
      var message = messages[j];
      message.markRead();
      //Get amount
      var match = regExAmount.exec(message.getSubject());
      
      var value = match[0].replace(/\s/g, "").replace(",","");
      
      Logger.log(value);
      deposit.push([message.getDate(), value]);
    }
  }
  deposit_sheet.getRange("A1:B" + deposit.length).setValues(deposit);
}

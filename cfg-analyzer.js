function Analyzer()
{
	var self = this;
	
	this.result = '';
	
	this.analyze = function(text)
	{
		var statements = [];
		var index = 0;
		var buffer = '';
		while (index < text.length)
		{
			var ch = text[index];
			
			if (ch == '"' || ch == '\'')
			{
				// TODO: escaped quotes.
				var endIndex = text.indexOf(ch, index + 1);
				buffer += text.substring(index, endIndex + 1);
				
				index = endIndex + 1;
			}
			else if (ch == '.')
			{
				index++;
				statements.push(buffer.trim());
				buffer = '';
			}
			else
			{
				buffer += ch;
				index++;
			}
		}
		
		statements = self.process(statements);
		
		var result = '';
		for (var index = 0; index < statements.length; index++)
		{
			result += '<p>'+ statements[index] + '</p>';
		}
		
		self.result = result;
	};
	
	this.process = function(statements)
	{
		var visualStatements = [];
		
		for (var index = 0; index < statements.length; index++)
		{
			var statement = statements[index];
			
			visualStatements.push(statement);
		}
		
		return visualStatements;
	};
	
	this.getResult = function()
	{
		return self.result;
	};
}

$(document).ready(function()
	{
		var analyzer = new Analyzer();
		
		$("#button").click(function()
			{
				analyzer.analyze($("#grammar").val());
				
				$("#result").html(analyzer.getResult());
			});
	});